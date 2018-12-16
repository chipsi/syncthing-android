package com.nutomic.syncthingandroid.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.nutomic.syncthingandroid.model.Device;
import com.nutomic.syncthingandroid.model.Folder;
import com.nutomic.syncthingandroid.R;
import com.nutomic.syncthingandroid.service.Constants;
import com.nutomic.syncthingandroid.service.SyncthingRunnable;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.mindrot.jbcrypt.BCrypt;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

/**
 * Provides direct access to the config.xml file in the file system.
 * This class should only be used if the syncthing API is not available (usually during startup).
 */
public class ConfigXml {

    public class OpenConfigException extends RuntimeException {
    }

    /**
     * Compares devices by name, uses the device ID as fallback if the name is empty
     */
    private final static Comparator<Device> DEVICES_COMPARATOR = (lhs, rhs) -> {
        String lhsName = lhs.name != null && !lhs.name.isEmpty() ? lhs.name : lhs.deviceID;
        String rhsName = rhs.name != null && !rhs.name.isEmpty() ? rhs.name : rhs.deviceID;
        return lhsName.compareTo(rhsName);
    };

    /**
     * Compares folders by labels, uses the folder ID as fallback if the label is empty
     */
    private final static Comparator<Folder> FOLDERS_COMPARATOR = (lhs, rhs) -> {
        String lhsLabel = lhs.label != null && !lhs.label.isEmpty() ? lhs.label : lhs.id;
        String rhsLabel = rhs.label != null && !rhs.label.isEmpty() ? rhs.label : rhs.id;
        return lhsLabel.compareTo(rhsLabel);
    };

    private static final String TAG = "ConfigXml";
    private static final int FOLDER_ID_APPENDIX_LENGTH = 4;

    private final Context mContext;

    @Inject
    SharedPreferences mPreferences;

    private final File mConfigFile;

    private Document mConfig;

    public ConfigXml(Context context) throws OpenConfigException, SyncthingRunnable.ExecutableNotFoundException {
        mContext = context;
        mConfigFile = Constants.getConfigFile(mContext);
        boolean isFirstStart = !mConfigFile.exists();
        if (isFirstStart) {
            Log.i(TAG, "App started for the first time. Generating keys and config.");
            new SyncthingRunnable(context, SyncthingRunnable.Command.generate).run(true);
        }

        readConfig();

        if (isFirstStart) {
            boolean changed = false;

            Log.i(TAG, "Starting syncthing to retrieve local device id.");
            String logOutput = new SyncthingRunnable(context, SyncthingRunnable.Command.deviceid).run(true);
            String localDeviceID = logOutput.replace("\n", "");
            // Verify local device ID is correctly formatted.
            if (localDeviceID.matches("^([A-Z0-9]{7}-){7}[A-Z0-9]{7}$")) {
                changed = changeLocalDeviceName(localDeviceID) || changed;
            }
            changed = changeDefaultFolder() || changed;

            // Save changes if we made any.
            if (changed) {
                saveChanges();
            }
        }
    }

    private void readConfig() {
        if (!mConfigFile.canRead() && !Util.fixAppDataPermissions(mContext)) {
            throw new OpenConfigException();
        }
        try {
            FileInputStream inputStream = new FileInputStream(mConfigFile);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
            InputSource inputSource = new InputSource(inputStreamReader);
            inputSource.setEncoding("UTF-8");
            DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Log.d(TAG, "Parsing config file '" + mConfigFile + "'");
            mConfig = db.parse(inputSource);
            inputStream.close();
            Log.i(TAG, "Successfully parsed config file");
        } catch (SAXException | ParserConfigurationException | IOException e) {
            Log.w(TAG, "Failed to parse config file '" + mConfigFile + "'", e);
            throw new OpenConfigException();
        }
    }

    public URL getWebGuiUrl() {
        String urlProtocol = Constants.osSupportsTLS12() ? "https" : "http";
        try {
            return new URL(urlProtocol + "://" + getGuiElement().getElementsByTagName("address").item(0).getTextContent());
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to parse web interface URL", e);
        }
    }

    public String getApiKey() {
        return getGuiElement().getElementsByTagName("apikey").item(0).getTextContent();
    }

    public String getUserName() {
        return getGuiElement().getElementsByTagName("user").item(0).getTextContent();
    }

    /**
     * Updates the config file.
     * Sets ignorePerms flag to true on every folder, force enables TLS, sets the
     * username/password, and disables weak hash checking.
     */
    @SuppressWarnings("SdCardPath")
    public void updateIfNeeded() {
        boolean changed = false;

        /* Perform one-time migration tasks on syncthing's config file when coming from an older config version. */
        changed = migrateSyncthingOptions() || changed;

        /* Get refs to important config objects */
        NodeList folders = mConfig.getDocumentElement().getElementsByTagName("folder");

        /* Section - folders */
        for (int i = 0; i < folders.getLength(); i++) {
            Element r = (Element) folders.item(i);
            // Set ignorePerms attribute.
            if (!r.hasAttribute("ignorePerms") ||
                    !Boolean.parseBoolean(r.getAttribute("ignorePerms"))) {
                Log.i(TAG, "Set 'ignorePerms' on folder " + r.getAttribute("id"));
                r.setAttribute("ignorePerms", Boolean.toString(true));
                changed = true;
            }

            // Set 'hashers' (see https://github.com/syncthing/syncthing-android/issues/384) on the
            // given folder.
            changed = setConfigElement(r, "hashers", "1") || changed;
        }

        /* Section - GUI */
        Element gui = getGuiElement();

        // Platform-specific: Force REST API and Web UI access to use TLS 1.2 or not.
        Boolean forceHttps = Constants.osSupportsTLS12();
        if (!gui.hasAttribute("tls") ||
                Boolean.parseBoolean(gui.getAttribute("tls")) != forceHttps) {
            gui.setAttribute("tls", forceHttps ? "true" : "false");
            changed = true;
        }

        // Set user to "syncthing"
        changed = setConfigElement(gui, "user", "syncthing") || changed;

        // Set password to the API key
        Node password = gui.getElementsByTagName("password").item(0);
        if (password == null) {
            password = mConfig.createElement("password");
            gui.appendChild(password);
        }
        String apikey = getApiKey();
        String pw = password.getTextContent();
        boolean passwordOk;
        try {
            passwordOk = !TextUtils.isEmpty(pw) && BCrypt.checkpw(apikey, pw);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Malformed password", e);
            passwordOk = false;
        }
        if (!passwordOk) {
            Log.i(TAG, "Updating password");
            password.setTextContent(BCrypt.hashpw(apikey, BCrypt.gensalt(4)));
            changed = true;
        }

        /* Section - options */
        // Disable weak hash benchmark for faster startup.
        // https://github.com/syncthing/syncthing/issues/4348
        Element options = (Element) mConfig.getDocumentElement()
                .getElementsByTagName("options").item(0);
        changed = setConfigElement(options, "weakHashSelectionMethod", "never") || changed;

        /* Dismiss "fsWatcherNotification" according to https://github.com/syncthing/syncthing-android/pull/1051 */
        NodeList childNodes = options.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("unackedNotificationID")) {
                if (node.equals("fsWatcherNotification")) {
                    Log.i(TAG, "Remove found unackedNotificationID 'fsWatcherNotification'.");
                    options.removeChild(node);
                    changed = true;
                    break;
                }
            }
        }

        // Save changes if we made any.
        if (changed) {
            saveChanges();
        }
    }

    /**
     * Updates syncthing options to a version specific target setting in the config file.
     * Used for one-time config migration from a lower syncthing version to the current version.
     * Enables filesystem watcher.
     * Returns if changes to the config have been made.
     */
    private boolean migrateSyncthingOptions() {
        /* Read existing config version */
        int iConfigVersion = Integer.parseInt(mConfig.getDocumentElement().getAttribute("version"));
        int iOldConfigVersion = iConfigVersion;
        Log.i(TAG, "Found existing config version " + Integer.toString(iConfigVersion));

        /* Check if we have to do manual migration from version X to Y */
        if (iConfigVersion == 27) {
            /* fsWatcher transition - https://github.com/syncthing/syncthing/issues/4882 */
            Log.i(TAG, "Migrating config version " + Integer.toString(iConfigVersion) + " to 28 ...");

            /* Enable fsWatcher for all folders */
            NodeList folders = mConfig.getDocumentElement().getElementsByTagName("folder");
            for (int i = 0; i < folders.getLength(); i++) {
                Element r = (Element) folders.item(i);

                // Enable "fsWatcherEnabled" attribute and set default delay.
                Log.i(TAG, "Set 'fsWatcherEnabled', 'fsWatcherDelayS' on folder " + r.getAttribute("id"));
                r.setAttribute("fsWatcherEnabled", "true");
                r.setAttribute("fsWatcherDelayS", "10");
            }

            /**
             * Set config version to 28 after manual config migration
             * This prevents "unackedNotificationID" getting populated
             * with the fsWatcher GUI notification.
             */
            iConfigVersion = 28;
        }

        if (iConfigVersion != iOldConfigVersion) {
            mConfig.getDocumentElement().setAttribute("version", Integer.toString(iConfigVersion));
            Log.i(TAG, "New config version is " + Integer.toString(iConfigVersion));
            return true;
        } else {
            return false;
        }
    }

    private Boolean getAttributeOrDefault(final Element element, String attribute, Boolean defaultValue) {
        return element.hasAttribute(attribute) ? Boolean.parseBoolean(element.getAttribute(attribute)) : defaultValue;
    }

    private Integer getAttributeOrDefault(final Element element, String attribute, Integer defaultValue) {
        return element.hasAttribute(attribute) ? Integer.parseInt(element.getAttribute(attribute)) : defaultValue;
    }

    private String getAttributeOrDefault(final Element element, String attribute, String defaultValue) {
        return element.hasAttribute(attribute) ? element.getAttribute(attribute) : defaultValue;
    }

    private Boolean getContentOrDefault(final Node node, Boolean defaultValue) {
         return (node == null) ? defaultValue : Boolean.parseBoolean(node.getTextContent());
    }

    public List<Folder> getFolders() {
        List<Folder> folders = new ArrayList<>();
        NodeList nodeFolders = mConfig.getDocumentElement().getElementsByTagName("folder");
        for (int i = 0; i < nodeFolders.getLength(); i++) {
            Element r = (Element) nodeFolders.item(i);
            Folder folder = new Folder();
            folder.id = getAttributeOrDefault(r, "id", "");
            folder.label = getAttributeOrDefault(r, "label", "");
            folder.path = getAttributeOrDefault(r, "path", "");
            folder.type = getAttributeOrDefault(r, "type", Constants.FOLDER_TYPE_SEND_RECEIVE);
            folder.paused = getContentOrDefault(r.getElementsByTagName("paused").item(0), false);
            // For testing purposes only.
            // Log.v(TAG, "folder.label=" + folder.label + "/" +"folder.type=" + folder.type + "/" + "folder.paused=" + folder.paused);
            folders.add(folder);
        }
        Collections.sort(folders, FOLDERS_COMPARATOR);
        return folders;
    }

    public List<Device> getDevices() {
        List<Device> devices = new ArrayList<>();
        NodeList nodeDevices = mConfig.getDocumentElement().getElementsByTagName("device");
        for (int i = 0; i < nodeDevices.getLength(); i++) {
            Element r = (Element) nodeDevices.item(i);
            Device device = new Device();
            device.deviceID = getAttributeOrDefault(r, "id", "");
            device.name = getAttributeOrDefault(r, "name", "");
            device.paused = getContentOrDefault(r.getElementsByTagName("paused").item(0), false);
            // For testing purposes only.
            // Log.v(TAG, "device.name=" + device.name + "/" +"device.id=" + device.deviceID + "/" + "device.paused=" + device.paused);
            devices.add(device);
        }
        Collections.sort(devices, DEVICES_COMPARATOR);
        return devices;
    }

    public void setFolderPause(String folderId, Boolean paused) {
        // ToDo
    }

    public void setDevicePause(String deviceId, Boolean paused) {
        // ToDo
    }

    private boolean setConfigElement(Element parent, String tagName, String textContent) {
        Node element = parent.getElementsByTagName(tagName).item(0);
        if (element == null) {
            element = mConfig.createElement(tagName);
            parent.appendChild(element);
        }
        if (!textContent.equals(element.getTextContent())) {
            element.setTextContent(textContent);
            return true;
        }
        return false;
    }

    private Element getGuiElement() {
        return (Element) mConfig.getDocumentElement().getElementsByTagName("gui").item(0);
    }

    /**
     * Set device model name as device name for Syncthing.
     * We need to iterate through XML nodes manually, as mConfig.getDocumentElement() will also
     * return nested elements inside folder element. We have to check that we only rename the
     * device corresponding to the local device ID.
     * Returns if changes to the config have been made.
     */
    private boolean changeLocalDeviceName(String localDeviceID) {
        NodeList childNodes = mConfig.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getNodeName().equals("device")) {
                if (((Element) node).getAttribute("id").equals(localDeviceID)) {
                    Log.i(TAG, "changeLocalDeviceName: Rename device ID " + localDeviceID + " to " + Build.MODEL);
                    ((Element) node).setAttribute("name", Build.MODEL);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Change default folder id to camera and path to camera folder path.
     * Returns if changes to the config have been made.
     */
    private boolean changeDefaultFolder() {
        Element folder = (Element) mConfig.getDocumentElement()
                .getElementsByTagName("folder").item(0);
        String deviceModel = Build.MODEL
                .replace(" ", "_")
                .toLowerCase(Locale.US)
                .replaceAll("[^a-z0-9_-]", "");
        String defaultFolderId = deviceModel + "_" + generateRandomString(FOLDER_ID_APPENDIX_LENGTH);
        folder.setAttribute("label", mContext.getString(R.string.default_folder_label));
        folder.setAttribute("id", mContext.getString(R.string.default_folder_id, defaultFolderId));
        folder.setAttribute("path", Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsolutePath());
        folder.setAttribute("type", Constants.FOLDER_TYPE_SEND_ONLY);
        folder.setAttribute("fsWatcherEnabled", "true");
        folder.setAttribute("fsWatcherDelayS", "10");
        return true;
    }

    /**
     * Generates a random String with a given length
     */
    private String generateRandomString(int length) {
        char[] chars = "abcdefghjkmnpqrstuvwxyz123456789".toCharArray();
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; ++i) {
            sb.append(chars[random.nextInt(chars.length)]);
        }
        return sb.toString();
    }

    /**
     * Writes updated mConfig back to file.
     */
    public void saveChanges() {
        if (!mConfigFile.canWrite() && !Util.fixAppDataPermissions(mContext)) {
            Log.w(TAG, "Failed to save updated config. Cannot change the owner of the config file.");
            return;
        }

        Log.i(TAG, "Writing updated config file");
        File mConfigTempFile = Constants.getConfigTempFile(mContext);
        try {
            // Write XML header.
            FileOutputStream fileOutputStream = new FileOutputStream(mConfigTempFile);
            fileOutputStream.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8"));

            // Prepare Object-to-XML transform.
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-16");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            // Output XML body.
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            StreamResult streamResult = new StreamResult(new OutputStreamWriter(byteArrayOutputStream, "UTF-8"));
            transformer.transform(new DOMSource(mConfig), streamResult);
            byte[] outputBytes = byteArrayOutputStream.toByteArray();
            fileOutputStream.write(outputBytes);
            fileOutputStream.close();
        } catch (TransformerException e) {
            Log.w(TAG, "Failed to transform object to xml and save temporary config file", e);
            return;
        } catch (FileNotFoundException e) {
            Log.w(TAG, "Failed to save temporary config file, FileNotFoundException", e);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Failed to save temporary config file, UnsupportedEncodingException", e);
        } catch (IOException e) {
            Log.w(TAG, "Failed to save temporary config file, IOException", e);
        }
        try {
            mConfigTempFile.renameTo(mConfigFile);
        } catch (Exception e) {
            Log.w(TAG, "Failed to rename temporary config file to original file");
        }
    }
}

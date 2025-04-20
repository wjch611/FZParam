package burp.plugin;

import burp.api.montoya.BurpExtension;
import burp.api.montoya.MontoyaApi;
import burp.api.montoya.http.message.requests.HttpRequest;
import burp.api.montoya.http.handler.*;
import burp.api.montoya.logging.Logging;
import burp.api.montoya.ui.UserInterface;

import javax.swing.*;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BurpExtender implements BurpExtension {
    private Logging logging;
    private volatile String outputFile = "E:\\SecTools\\auto_param\\FZParam.txt"; // Changed default file name to FZParam.txt
    private volatile String hostRegex = ".*example\\.com$";
    private volatile Pattern hostPattern;
    private volatile boolean isLoggingEnabled = false;
    private final Set<String> loggedUrls = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("FZParam"); // Changed plugin name to FZParam
        logging = api.logging();

        // Initialize default regex pattern
        try {
            hostPattern = Pattern.compile(hostRegex);
        } catch (PatternSyntaxException e) {
            logging.logToError("Invalid default regex: " + hostRegex + ". Falling back to .*$");
            hostRegex = ".*$";
            hostPattern = Pattern.compile(hostRegex);
        }

        // Register HTTP handler
        api.http().registerHttpHandler(new UrlLoggingHandler());

        // Create and register UI
        UserInterface userInterface = api.userInterface();
        userInterface.registerSuiteTab("FZParam", createConfigPanel()); // Changed tab name to FZParam

        logging.logToOutput("FZParam Extension loaded. Initial output file: " + outputFile);
        logging.logToOutput("Initial host filter regex: " + hostRegex);
        logging.logToOutput("Logging is initially stopped.");
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Host Regex Input
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Host Regex:"), gbc);

        JTextField regexField = new JTextField(hostRegex, 30);
        gbc.gridx = 1;
        panel.add(regexField, gbc);

        // Output File Input
        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Output File:"), gbc);

        JTextField fileField = new JTextField(outputFile, 30);
        gbc.gridx = 1;
        panel.add(fileField, gbc);

        // Browse Button
        JButton browseButton = new JButton("Browse...");
        gbc.gridx = 2;
        panel.add(browseButton, gbc);
        browseButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (fileChooser.showSaveDialog(panel) == JFileChooser.APPROVE_OPTION) {
                fileField.setText(fileChooser.getSelectedFile().getAbsolutePath());
            }
        });

        // Start/Stop Toggle Button
        JToggleButton toggleButton = new JToggleButton("Start Logging", isLoggingEnabled);
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(toggleButton, gbc);
        toggleButton.addActionListener(e -> {
            boolean newState = toggleButton.isSelected();
            if (newState) {
                // Starting logging: validate and save settings
                String newRegex = regexField.getText().trim();
                String newFile = fileField.getText().trim();

                // Validate regex
                try {
                    Pattern.compile(newRegex);
                    hostRegex = newRegex;
                    hostPattern = Pattern.compile(newRegex);
                    logging.logToOutput("Updated host regex to: " + newRegex);
                } catch (PatternSyntaxException ex) {
                    logging.logToError("Invalid regex: " + newRegex);
                    JOptionPane.showMessageDialog(panel, "Invalid regex pattern: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    toggleButton.setSelected(false); // Revert toggle
                    return;
                }

                // Validate file path
                try {
                    File file = new File(newFile).getParentFile();
                    if (file != null && !file.exists()) {
                        file.mkdirs();
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(newFile, true))) {
                        // Test write
                    }
                    outputFile = newFile;
                    loggedUrls.clear(); // Clear deduplication set for new file
                    logging.logToOutput("Updated output file to: " + newFile);
                } catch (IOException ex) {
                    logging.logToError("Invalid file path: " + newFile + ". Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(panel, "Cannot write to file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    toggleButton.setSelected(false); // Revert toggle
                    return;
                }
            }

            // Update logging state
            isLoggingEnabled = newState;
            toggleButton.setText(isLoggingEnabled ? "Stop Logging" : "Start Logging");
            logging.logToOutput(isLoggingEnabled ? "Logging started." : "Logging stopped.");
        });

        return panel;
    }

    private class UrlLoggingHandler implements HttpHandler {
        @Override
        public RequestToBeSentAction handleHttpRequestToBeSent(HttpRequestToBeSent requestToBeSent) {
            processRequest(requestToBeSent);
            return RequestToBeSentAction.continueWith(requestToBeSent);
        }

        @Override
        public ResponseReceivedAction handleHttpResponseReceived(HttpResponseReceived responseReceived) {
            return ResponseReceivedAction.continueWith(responseReceived);
        }

        private void processRequest(HttpRequest request) {
            // Skip processing if logging is disabled
            if (!isLoggingEnabled) {
                return;
            }

            String url = request.url();
            String host = request.httpService().host();

            // Check for query parameters
            if (!url.contains("?")) {
                return;
            }

            // Apply host regex filter
            if (!hostPattern.matcher(host).matches()) {
                logging.logToOutput("Skipping URL (host does not match regex): " + url);
                return;
            }

            // Fuzz query parameter values
            String modifiedUrl = fuzzQueryParameters(url);

            // Deduplicate URLs
            if (!loggedUrls.add(modifiedUrl)) {
                logging.logToOutput("Skipping duplicate URL: " + modifiedUrl);
                return;
            }

            // Write to file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                String logEntry = String.format("%s%n", modifiedUrl);
                writer.write(logEntry);
                logging.logToOutput("Logged URL: " + modifiedUrl);
            } catch (IOException e) {
                logging.logToError("Failed to write to file " + outputFile + ": " + e.getMessage());
            }
        }

        private String fuzzQueryParameters(String url) {
            // Split URL into base and query string
            int queryIndex = url.indexOf('?');
            if (queryIndex == -1) {
                return url; // No query parameters, return unchanged
            }

            String baseUrl = url.substring(0, queryIndex);
            String queryString = url.substring(queryIndex + 1);

            // Split query string into parameters
            String[] params = queryString.split("&");
            StringBuilder fuzzedQuery = new StringBuilder();

            for (int i = 0; i < params.length; i++) {
                String param = params[i];
                int equalsIndex = param.indexOf('=');
                if (equalsIndex == -1) {
                    // Parameter without value (e.g., "key")
                    fuzzedQuery.append(param);
                } else {
                    // Parameter with value (e.g., "key=value")
                    String key = param.substring(0, equalsIndex);
                    fuzzedQuery.append(key).append("=FUZZ");
                }
                if (i < params.length - 1) {
                    fuzzedQuery.append("&");
                }
            }

            return baseUrl + "?" + fuzzedQuery.toString();
        }
    }
}
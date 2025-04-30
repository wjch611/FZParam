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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class BurpExtender implements BurpExtension {
    private Logging logging;
    private volatile String outputFile = "E:\\SecTools\\auto_param\\FZParam.txt";
    private volatile List<Pattern> hostPatterns;
    private volatile boolean isLoggingEnabled = false;
    private final Set<String> loggedUrls = Collections.synchronizedSet(new HashSet<>());
    private final List<JTextField> regexFields = Collections.synchronizedList(new ArrayList<>());
    private JPanel regexPanel;

    @Override
    public void initialize(MontoyaApi api) {
        api.extension().setName("FZParam");
        logging = api.logging();

        // Initialize default regex pattern
        hostPatterns = new ArrayList<>();
        try {
            hostPatterns.add(Pattern.compile(".*example\\.com$"));
        } catch (PatternSyntaxException e) {
            logging.logToError("Invalid default regex: .*example\\.com$. Falling back to .*$");
            hostPatterns.add(Pattern.compile(".*$"));
        }

        // Register HTTP handler
        api.http().registerHttpHandler(new UrlLoggingHandler());

        // Create and register UI
        UserInterface userInterface = api.userInterface();
        userInterface.registerSuiteTab("FZParam", createConfigPanel());

        logging.logToOutput("FZParam Extension loaded. Output file: " + outputFile);
        logging.logToOutput("Initial host regex: .*example\\.com$");
        logging.logToOutput("Logging is stopped.");
    }

    private JPanel createConfigPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Add "+" button for more regex fields
        JButton addRegexButton = new JButton("+");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(addRegexButton, gbc);
        addRegexButton.addActionListener(e -> {
            synchronized (regexFields) {
                if (regexFields.size() < 10) {
                    addRegexField("");
                    regexPanel.revalidate();
                    regexPanel.repaint();
                } else {
                    JOptionPane.showMessageDialog(panel, "Maximum 10 host regex patterns allowed.", "Limit Reached", JOptionPane.WARNING_MESSAGE);
                }
            }
        });

        // Host Regex Section
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(new JLabel("Host Regex:"), gbc);

        regexPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1.0;
        panel.add(regexPanel, gbc);

        // Add initial regex field
        addRegexField(".*example\\.com$");

        // Output File Input
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(new JLabel("Output File:"), gbc);

        JTextField fileField = new JTextField(outputFile, 30);
        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(fileField, gbc);

        // Browse Button
        JButton browseButton = new JButton("Browse...");
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
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
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(toggleButton, gbc);
        toggleButton.addActionListener(e -> {
            boolean newState = toggleButton.isSelected();
            if (newState) {
                // Starting logging: validate and save settings
                List<String> newRegexes = new ArrayList<>();
                synchronized (regexFields) {
                    for (JTextField regexField : regexFields) {
                        String regex = regexField.getText().trim();
                        if (!regex.isEmpty()) {
                            newRegexes.add(regex);
                        }
                    }
                }

                // Validate regexes
                List<Pattern> newPatterns = new ArrayList<>();
                for (String regex : newRegexes) {
                    try {
                        newPatterns.add(Pattern.compile(regex));
                        logging.logToOutput("Validated host regex: " + regex);
                    } catch (PatternSyntaxException ex) {
                        logging.logToError("Invalid regex: " + regex + ". Error: " + ex.getMessage());
                        JOptionPane.showMessageDialog(panel, "Invalid regex pattern: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        toggleButton.setSelected(false);
                        return;
                    }
                }

                // Validate file path
                String newFile = fileField.getText().trim();
                try {
                    File file = new File(newFile).getParentFile();
                    if (file != null && !file.exists()) {
                        file.mkdirs();
                    }
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(newFile, true))) {
                        // Test write
                    }
                    outputFile = newFile;
                    loggedUrls.clear();
                    logging.logToOutput("Output file set to: " + newFile);
                } catch (IOException ex) {
                    logging.logToError("Invalid file path: " + newFile + ". Error: " + ex.getMessage());
                    JOptionPane.showMessageDialog(panel, "Cannot write to file: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                    toggleButton.setSelected(false);
                    return;
                }

                // Update patterns
                hostPatterns = newPatterns;
            }

            // Update logging state
            isLoggingEnabled = newState;
            toggleButton.setText(isLoggingEnabled ? "Stop Logging" : "Start Logging");
            logging.logToOutput(isLoggingEnabled ? "Logging started." : "Logging stopped.");
        });

        return panel;
    }

    private void addRegexField(String initialText) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 2, 2, 2);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField regexField = new JTextField(initialText, 30);
        gbc.gridx = 0;
        gbc.gridy = regexFields.size();
        gbc.weightx = 1.0;
        regexPanel.add(regexField, gbc);
        synchronized (regexFields) {
            regexFields.add(regexField);
        }

        if (regexFields.size() > 1) {
            JButton removeButton = new JButton("-");
            gbc.gridx = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            regexPanel.add(removeButton, gbc);
            removeButton.addActionListener(e -> {
                synchronized (regexFields) {
                    regexFields.remove(regexField);
                    regexPanel.remove(regexField);
                    regexPanel.remove(removeButton);
                    regexPanel.revalidate();
                    regexPanel.repaint();
                }
            });
        }
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
            if (!isLoggingEnabled || hostPatterns.isEmpty()) {
                return;
            }

            String url = request.url();
            String host = request.httpService().host();

            if (!url.contains("?")) {
                return;
            }

            boolean hostMatches = hostPatterns.stream().anyMatch(pattern -> pattern.matcher(host).matches());
            if (!hostMatches) {
                logging.logToOutput("Skipping URL (host does not match): " + url);
                return;
            }

            String modifiedUrl = fuzzQueryParameters(url);

            if (!loggedUrls.add(modifiedUrl)) {
                logging.logToOutput("Skipping duplicate URL: " + modifiedUrl);
                return;
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile, true))) {
                writer.write(modifiedUrl + "\n");
                logging.logToOutput("Logged URL: " + modifiedUrl);
            } catch (IOException e) {
                logging.logToError("Failed to write to file " + outputFile + ": " + e.getMessage());
            }
        }

        private String fuzzQueryParameters(String url) {
            int queryIndex = url.indexOf('?');
            if (queryIndex == -1) {
                return url;
            }

            String baseUrl = url.substring(0, queryIndex);
            String queryString = url.substring(queryIndex + 1);
            String[] params = queryString.split("&");
            StringBuilder fuzzedQuery = new StringBuilder();

            for (int i = 0; i < params.length; i++) {
                if (params[i].isEmpty()) {
                    continue;
                }
                fuzzedQuery.append(params[i].contains("=") ? params[i].substring(0, params[i].indexOf('=') + 1) : params[i] + "=");
                fuzzedQuery.append("FUZZ");
                if (i < params.length - 1) {
                    fuzzedQuery.append("&");
                }
            }

            return baseUrl + "?" + fuzzedQuery;
        }
    }
}
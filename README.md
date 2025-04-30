FZParam - Burp Suite Extension
FZParam is a Burp Suite extension designed to assist in web application security testing by logging URLs with query parameters and fuzzing them for further analysis. It allows users to specify host patterns using regular expressions, customize the output file for logged URLs, and toggle logging on or off via a user-friendly interface.
Features
1. URL Logging

Captures HTTP requests containing query parameters (e.g., example.com/path?a=1&b=2).
Logs modified URLs to a specified output file after replacing query parameter values with FUZZ (e.g., example.com/path?a=FUZZ&b=FUZZ).
Avoids duplicate logging by maintaining a set of already logged URLs.

2. Host Filtering

Filters requests based on host patterns defined using regular expressions.
Supports up to 10 regex patterns for host matching.
Default pattern: .*example\.com$ (can be modified in the UI).

3. Customizable Output

Allows users to specify the output file path for logged URLs.
Validates the file path and ensures the directory exists before writing.

4. User Interface

Provides a dedicated tab (FZParam) in Burp Suite for configuration.
Features:
Add/remove host regex patterns dynamically (up to 10).
Browse and set the output file path.
Start/stop logging with a toggle button.


Displays validation errors for invalid regex patterns or file paths.

5. Logging Control

Toggle logging on or off without unloading the extension.
Clears the duplicate URL cache when logging is restarted.

Installation
Prerequisites

Burp Suite Professional or Community Edition (compatible with the Montoya API).
Java Development Kit (JDK): Required to build the plugin.
Maven or Gradle: Recommended for building the plugin (if using a build tool).

Steps

Clone or Download the Repository

Clone this repository or download the source code:git clone <repository-url>




Build the Plugin

Navigate to the project directory and build the plugin using your preferred build tool.
If using Maven, run:mvn clean package


The compiled JAR file will be in the target/ directory (e.g., FZParam.jar).


Load the Plugin in Burp Suite

Open Burp Suite.
Go to the Extensions tab (or Extender in older versions).
Click Add, select Java as the extension type, and browse to the compiled FZParam.jar file.
The plugin should load, and a new tab named FZParam will appear in the Burp Suite UI.



Usage
1. Configure the Plugin

Open the FZParam Tab:
After loading the plugin, navigate to the FZParam tab in Burp Suite.


Set Host Regex Patterns:
The default regex is .*example\.com$. Modify it or add more patterns (up to 10) by clicking the + button.
Example: To match *.example.com and *.test.com, add:.*\.example\.com$
.*\.test\.com$


Click the - button next to a regex field to remove it.


Set Output File Path:
The default output file is E:\SecTools\auto_param\FZParam.txt.
Use the Browse... button to select a different file path, or manually enter a path in the text field.


Start Logging:
Click the Start Logging button to begin capturing URLs.
The button will change to Stop Logging when logging is active.



2. Monitor Output

Check the Burp Suite Output tab (under Extensions > FZParam) for logs:
Successful URL logging: Logged URL: <modified-url>
Skipped URLs: Skipping URL (host does not match): <url> or Skipping duplicate URL: <url>
Errors: Invalid regex: <regex> or Failed to write to file: <error>


The logged URLs will be written to the specified output file with query parameters fuzzed (e.g., example.com/path?a=FUZZ&b=FUZZ).

3. Stop Logging

Click the Stop Logging button to pause URL logging.
You can restart logging at any time, which will clear the duplicate URL cache.

Example
Scenario
You want to log URLs from example.com and test.com domains with query parameters, fuzz the parameters, and save the results to a file.
Steps

Configure Host Regex:
In the FZParam tab, set the following regex patterns:.*\.example\.com$
.*\.test\.com$




Set Output File:
Specify the output file as C:\temp\fuzzed_urls.txt.


Start Logging:
Click Start Logging.


Send Requests:
Use Burp Suite’s Proxy or Repeater to send requests like:GET http://sub.example.com/path?a=1&b=2
GET http://app.test.com/login?user=admin&pass=123
GET http://other.com/no-match?x=5




Check Output:
The file C:\temp\fuzzed_urls.txt will contain:http://sub.example.com/path?a=FUZZ&b=FUZZ
http://app.test.com/login?user=FUZZ&pass=FUZZ


The other.com URL will be skipped because it doesn’t match the host patterns.



Notes
1. Regex Validation

Ensure your regex patterns are valid. Invalid patterns (e.g., *.example.com without escaping the dot) will prevent logging from starting, and an error message will be shown.

2. Output File Permissions

The plugin validates the output file path before starting logging. If the directory doesn’t exist, it will attempt to create it. If the file cannot be written to (e.g., due to permissions), an error will be displayed.

3. Duplicate URLs

The plugin avoids logging duplicate URLs to prevent redundant entries. The duplicate cache is cleared when logging is restarted.

4. Performance

The plugin is designed to handle typical web traffic, but logging a large number of requests with many query parameters may impact performance. Use specific regex patterns to filter hosts and reduce the number of logged URLs.

Troubleshooting
1. "Invalid regex pattern" Error

Cause: The regex pattern is syntactically incorrect (e.g., unescaped special characters).
Solution: Fix the regex pattern. For example, use .*\.example\.com$ instead of *.example.com.

2. "Cannot write to file" Error

Cause: The specified output file path is invalid or lacks write permissions.
Solution: Ensure the directory exists and you have write permissions. Use the Browse... button to select a valid path.

3. No URLs Logged

Cause: Logging is disabled, or the host patterns don’t match any requests.
Solution:
Ensure the Start Logging button is toggled on.
Verify that your regex patterns match the target hosts (check the Output tab for skipped URLs).



Contributing
If you encounter bugs or have feature requests, please submit an issue on the GitHub repository. Contributions are welcome via pull requests.
License
This project is licensed under the MIT License. See the LICENSE file for details.

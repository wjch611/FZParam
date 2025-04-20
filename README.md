# FZParam
A Burp plugin that collects and fuzzes parameterized URLs in real time.

A simple and efficient Burp Suite extension that extracts parameterized URLs in real time, replaces parameter values with `FUZZ`, and writes them to a local file — perfect for fuzzing workflows.

---

# 🧩 Features

- 🎯 Realtime capture of **URLs with parameters** from Burp traffic
- 🛠 Replaces parameter values with `FUZZ` for easy fuzzing
- 🔍 Supports **host filtering** via regular expressions
- 📝 Outputs to local file, compatible with tools like `ffuf`, `qsreplace`, etc.


# 📤 Output Format

The output is written in real time to a local file like this:

https://cloud.oppo.com/login.html?callback=FUZZ
https://cloud.oppo.com/third/language/oppolang/zh-cn.js?r=FUZZ
https://obus-cn.dc.heytapmobi.com/v3/track/js/116600?app_key=FUZZ&timestamp=FUZZ
https://obus-cn.dc.heytapmobi.com/v3/balance/js/116600?app_key=FUZZ&timestamp=FUZZ


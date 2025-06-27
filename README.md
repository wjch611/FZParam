

---

## FZParam - Burp Suite 插件

**FZParam** 是一个为 Web 应用安全测试设计的 Burp Suite 插件，功能包括记录带参数的 URL 并对其进行模糊测试（fuzzing）以辅助分析。用户可以通过正则表达式指定主机匹配规则、自定义输出文件路径，并通过友好的界面启用或禁用记录功能。

---

## 功能介绍

### 1. URL 记录

* 捕获包含查询参数的 HTTP 请求（如 `example.com/path?a=1&b=2`）。
* 将参数值替换为 `FUZZ` 后记录到指定输出文件（如 `example.com/path?a=FUZZ&b=FUZZ`）。
* 利用集合去重机制避免重复记录。

### 2. 主机过滤

* 根据用户定义的正则表达式匹配主机进行过滤。
* 支持最多 10 条主机正则规则。
* 默认规则：`.*example\.com$`（可在 UI 中修改）。

### 3. 输出自定义

* 允许用户指定输出文件的路径。
* 在写入之前会验证路径是否有效并确保目录存在。

### 4. 图形界面

插件将在 Burp Suite 中添加一个专属标签页 **FZParam**，包含如下功能：

* 动态添加/删除主机正则规则（最多 10 条）。
* 选择并设置输出文件路径。
* 启动或停止记录的开关按钮。
* 对无效的正则表达式或文件路径显示错误提示。

### 5. 日志控制

* 可以在不卸载插件的情况下启用或禁用记录功能。
* 重新启用记录功能时，会清空已记录的 URL 缓存。

---

## 安装指南

### 先决条件

* Burp Suite 专业版或社区版（兼容 Montoya API）。
* Java 开发工具包（JDK）。
* 推荐使用 Maven 或 Gradle 进行构建。

### 安装步骤

1. **克隆或下载代码库**

```bash
git clone <repository-url>
```

2. **构建插件**

进入项目目录，使用构建工具构建插件。

* 如果使用 Maven：

```bash
mvn clean package
```

构建完成后，JAR 文件将生成在 `target/` 目录下（如：`FZParam.jar`）。

3. **加载插件到 Burp Suite**

* 打开 Burp Suite。
* 转到 **Extensions**（或旧版中的 **Extender**）标签页。
* 点击 **Add**，选择类型为 **Java**，浏览选择刚编译好的 `FZParam.jar` 文件。
* 插件加载成功后，将出现新的标签页 **FZParam**。

---

## 使用说明

### 1. 配置插件

#### 打开 FZParam 标签页

插件加载后，在 Burp Suite 中进入 FZParam 标签页。

#### 设置主机正则规则

* 默认规则为 `.*example\.com$`。
* 可点击 **+** 按钮添加更多规则（最多 10 条）。
* 例如，匹配 `*.example.com` 和 `*.test.com` 可设置为：

```
.*\.example\.com$
.*\.test\.com$
```

* 点击 **-** 可删除对应的规则。

#### 设置输出文件路径

* 默认路径为 `E:\SecTools\auto_param\FZParam.txt`。
* 可点击 **Browse...** 按钮选择路径，或手动输入完整路径。

#### 启动记录

* 点击 **Start Logging** 启动记录功能。
* 按钮变为 **Stop Logging** 表示记录已开启。

---

### 2. 查看输出

在 Burp 的 **Output**（输出）标签页中可查看日志：

* 成功记录：`Logged URL: <modified-url>`
* 被跳过的 URL：

  * 主机不匹配：`Skipping URL (host does not match): <url>`
  * 重复 URL：`Skipping duplicate URL: <url>`
* 错误提示：

  * 正则无效：`Invalid regex: <regex>`
  * 写入失败：`Failed to write to file: <error>`

记录的 URL 会写入到指定文件，参数值会被替换为 `FUZZ`，如：

```
example.com/path?a=FUZZ&b=FUZZ
```

---

### 3. 停止记录

点击 **Stop Logging** 按钮即可暂停记录。

重新点击 **Start Logging** 可以重新开始记录，同时清空 URL 去重缓存。

---

## 使用示例

### 场景：

你想记录来自 `example.com` 和 `test.com` 域名下的带参数 URL，将参数替换为 `FUZZ` 后保存至文件。

### 步骤：

1. **配置主机正则规则**

```
.*\.example\.com$
.*\.test\.com$
```

2. **设置输出文件路径**

例如设为：

```
C:\temp\fuzzed_urls.txt
```

3. **开始记录**

点击 **Start Logging**。

4. **发送请求**

在 Burp 中通过代理或 Repeater 发出如下请求：

```
GET http://sub.example.com/path?a=1&b=2
GET http://app.test.com/login?user=admin&pass=123
GET http://other.com/no-match?x=5
```

5. **查看输出**

文件 `C:\temp\fuzzed_urls.txt` 内容应为：

```
http://sub.example.com/path?a=FUZZ&b=FUZZ
http://app.test.com/login?user=FUZZ&pass=FUZZ
```

来自 `other.com` 的 URL 不符合规则，因此被跳过。

---

## 注意事项

### 1. 正则表达式校验

* 正则表达式必须合法。
* 如：`*.example.com` 是无效的，应使用 `.*\.example\.com$`。

### 2. 输出文件权限

* 插件会验证路径是否有效并尝试创建目录。
* 若路径无效或无写入权限，将显示错误提示。

### 3. 去重机制

* 插件使用缓存避免记录重复 URL。
* 每次重新开始记录时，会清空缓存。

### 4. 性能提示

* 插件可处理常规流量，但若请求数量极多、参数非常多，可能会影响性能。
* 建议使用具体的主机正则表达式缩小匹配范围。

---

## 常见问题与故障排查

### 1. “Invalid regex pattern” 错误

* **原因**：正则语法错误。
* **解决**：使用合法语法，如：`.*\.example\.com$`。

### 2. “Cannot write to file” 错误

* **原因**：路径不存在或没有写权限。
* **解决**：确保目录存在且有写权限，建议使用 **Browse...** 按钮选择路径。

### 3. 无 URL 被记录

* **原因**：

  * 记录功能未启用；
  * 主机规则未匹配。
* **解决**：

  * 确保点击了 **Start Logging**；
  * 检查正则表达式是否正确匹配请求主机（可查看 Output 输出）。

---

## 贡献说明

若发现漏洞或有功能建议，欢迎在 GitHub 提 issue 或通过 Pull Request 贡献代码。

---

## 授权协议

本项目基于 MIT 开源许可证，详情请参阅项目中的 LICENSE 文件。

---

如果你希望我帮你编写这个插件、部署测试、完善 README 或制作教学资料，也可以继续提问。

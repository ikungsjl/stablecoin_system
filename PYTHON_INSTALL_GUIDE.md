# Python 安装指南

## 快速安装

### 方式 1：自动安装（推荐）

#### Windows 用户

**使用 PowerShell 脚本**（推荐）：

```powershell
# 1. 打开 PowerShell（以管理员身份）
# 2. 进入项目目录
cd c:\Users\admin\Desktop\stablecoin_system

# 3. 运行安装脚本
.\install_python.ps1
```

**或使用批处理脚本**：

```cmd
# 1. 打开命令提示符（以管理员身份）
# 2. 进入项目目录
cd c:\Users\admin\Desktop\stablecoin_system

# 3. 运行安装脚本
install_python.bat
```

### 方式 2：手动安装

#### 步骤 1：下载 Python

访问 https://www.python.org/downloads/ 下载 Python 3.12

#### 步骤 2：安装 Python

1. 运行下载的安装程序
2. **重要**：勾选 "Add Python to PATH"
3. 选择 "Install Now" 或自定义安装
4. 等待安装完成

#### 步骤 3：验证安装

打开命令提示符或 PowerShell，运行：

```powershell
python --version
python -m pip --version
```

应该看到类似输出：

```
Python 3.12.0
pip 23.3.1 from C:\Python312\lib\site-packages\pip (python 3.12)
```

#### 步骤 4：安装项目依赖

```powershell
cd c:\Users\admin\Desktop\stablecoin_system
python -m pip install -r requirements.txt
```

## 验证安装

安装完成后，验证所有依赖都已安装：

```powershell
python -m pip list
```

应该看到：

```
scikit-learn       1.3.2
numpy              1.24.3
pandas             2.0.3
joblib             1.3.2
```

## 运行 ML 模块

### 训练模型

```powershell
cd c:\Users\admin\Desktop\stablecoin_system
python risk_predictor.py
```

这会：
- 生成 1000 个合成样本
- 训练随机森林模型
- 评估模型性能
- 保存模型到 `risk_predictor_model.pkl`

### 预期输出

```
==================================================
步骤 1: 生成训练数据
==================================================
[DataGenerator] 生成了 1000 个合成样本

==================================================
步骤 2: 训练模型
==================================================
[RiskPredictor] 随机森林模型已初始化
[RiskPredictor] 模型训练完成
  准确率: 0.8500
  精确率: 0.8400
  召回率: 0.8500
  F1 分数: 0.8400

==================================================
步骤 3: 特征重要性
==================================================
  reserve_ratio: 0.3500
  low_risk_ratio: 0.2500
  alert_count: 0.1500
  ...

==================================================
步骤 4: 风险预测
==================================================
预测风险等级: HEALTHY
置信度: 0.9200

==================================================
步骤 5: 综合风险分析
==================================================
规则判断: HEALTHY
ML 预测: HEALTHY (置信度: 0.9200)
最终风险等级: HEALTHY

==================================================
步骤 6: 保存模型
==================================================
[RiskPredictor] 模型已保存到 risk_predictor_model.pkl
```

## 故障排除

### 问题 1：PowerShell 执行策略错误

**错误信息**：
```
无法加载文件 install_python.ps1，因为在此系统上禁止运行脚本
```

**解决方案**：

```powershell
# 以管理员身份运行 PowerShell，然后执行：
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### 问题 2：Python 找不到

**错误信息**：
```
'python' 不是内部或外部命令
```

**解决方案**：

1. 重新启动 PowerShell 或命令提示符
2. 或使用完整路径：`C:\Python312\python.exe --version`
3. 或重新安装 Python，确保勾选 "Add Python to PATH"

### 问题 3：pip 安装失败

**错误信息**：
```
ERROR: Could not find a version that satisfies the requirement
```

**解决方案**：

```powershell
# 升级 pip
python -m pip install --upgrade pip

# 重试安装
python -m pip install -r requirements.txt
```

### 问题 4：网络连接问题

如果自动脚本下载失败，可以手动下载：

1. 访问 https://www.python.org/downloads/
2. 下载 Python 3.12.0 (64-bit)
3. 运行安装程序
4. 勾选 "Add Python to PATH"
5. 完成安装后运行：`python -m pip install -r requirements.txt`

## 环境变量配置

如果 Python 安装后仍无法识别，可以手动添加到 PATH：

### Windows 10/11

1. 打开 "系统属性" → "环境变量"
2. 在 "系统变量" 中找到 "Path"
3. 点击 "编辑"
4. 添加：`C:\Python312`
5. 添加：`C:\Python312\Scripts`
6. 点击 "确定"
7. 重启 PowerShell 或命令提示符

## 下一步

安装完成后，可以：

1. **启用 ML 预测**：修改 `application.yml`
   ```yaml
   app:
     ml:
       enabled: true
   ```

2. **重启 Spring Boot 应用**

3. **系统会自动使用 ML 模型进行风险预测**

## 参考资源

- Python 官网：https://www.python.org/
- Scikit-learn 文档：https://scikit-learn.org/
- pip 文档：https://pip.pypa.io/

## 获取帮助

如果遇到问题，请：

1. 检查 Python 版本：`python --version`
2. 检查 pip 版本：`python -m pip --version`
3. 查看已安装的包：`python -m pip list`
4. 查看详细错误信息：`python -m pip install -r requirements.txt -v`

---

**最后更新**：2026-03-28

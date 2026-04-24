# ArkOps-Ai Skill 模板

这是一个用于开发 ArkOps-Ai Skill 的模板项目。

## 快速开始

### 1. 复制模板

```bash
cp -r skill-template my-new-skill
cd my-new-skill
```

### 2. 修改 pom.xml

修改以下字段：
- `groupId`: 你的包名（如 `com.yourname.myskill`）
- `artifactId`: Skill 名称
- `version`: 版本号

### 3. 实现 Skill

编辑 `src/main/java/com/example/myskill/MySkill.java`：

```java
public class MySkill implements Skill, Listener {
    
    @Override
    public String getId() {
        return "my_skill_id";
    }
    
    @Override
    public void onEnable(JavaPlugin mainPlugin) {
        // 使用 mainPlugin 注册事件
        Bukkit.getPluginManager().registerEvents(this, mainPlugin);
    }
}
```

### 4. 编译

```bash
mvn clean package
```

### 5. 部署

将生成的 jar 文件放入：
```
plugins/ArkOps-Ai/skills/
```

### 6. 重启服务器

启动 Minecraft 服务器，检查日志：
```
[ArkOps-Ai] 已注册 Skill: My Skill v1.0.0 by Your Name
```

## 重要提示

- Skill ≠ Bukkit Plugin
- 注册事件必须使用 `mainPlugin`
- 必须有无参构造函数
- 依赖作用域设为 `provided`

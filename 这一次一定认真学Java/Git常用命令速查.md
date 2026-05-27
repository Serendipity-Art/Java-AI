# Git 常用命令速查

## 基础三件套（日常开发）
```bash
git add .                     # 把改动加入暂存区
git commit -m "描述"          # 提交到本地仓库
git push origin main          # 同步到 GitHub
```

## 分支操作
```bash
git branch                    # 查看本地所有分支
git branch -a                 # 查看所有分支（含远程）
git checkout -b 分支名        # 创建并切换到新分支
git checkout 分支名           # 切换到已有分支
git merge 分支名              # 把指定分支合并到当前分支
```

## 撤销与回溯
```bash
git reset --hard HEAD~1       # 撤回最近一次提交（本地）
git revert HEAD               # 安全撤销（生成一个新提交）
git checkout -- 文件名        # 丢弃文件未暂存的修改
```

## 远程同步
```bash
git push origin main          # 推送到远程
git pull origin main          # 从远程拉取最新代码
git fetch                     # 查看远程有什么新东西（不自动合并）
```

## 查看信息
```bash
git status                    # 看当前状态（改了哪些文件）
git log --oneline             # 看提交历史（简洁版）
git log --oneline --graph     # 看分支图谱
git diff                      # 看具体改了什么
```

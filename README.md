
### 工程说明



### 部署
- 正式环境发布，版本号不带snapshot
- 正式环境和非正式环境deploy，都要升级版本号

部署命令
```shell
mvn deploy "-Dmaven.test.skip=true" -N
```
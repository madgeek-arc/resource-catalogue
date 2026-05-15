## [5.3.0](https://github.com/madgeek-arc/resource-catalogue/compare/v5.2.0...v5.3.0) (2025-11-06)


### Features

* accounting service functionality ([0e4640c](https://github.com/madgeek-arc/resource-catalogue/commit/0e4640cc3d232541fbe9145f429fad0e86454509))
* helpdesk functionallity ([547e34c](https://github.com/madgeek-arc/resource-catalogue/commit/547e34cbc6db79cec8a17d532805f69eb7dbff09))
* **logging:** replace logging pattern and use 'request-logging' lib to display rich access logs respecting user privacy ([5153816](https://github.com/madgeek-arc/resource-catalogue/commit/5153816196af080655149200f936406eccbaa281))
* **wizard:** basic wizard functionality ([cc02723](https://github.com/madgeek-arc/resource-catalogue/commit/cc0272381b1b3b3a5ad11eb3038d14e627176f41))


### Bug Fixes

* ensures the return of non-public Configuration Template Instances through the controller ([36c8d92](https://github.com/madgeek-arc/resource-catalogue/commit/36c8d927c58c275bb934eefa7ce84b6ec7d29457))
* inits web client ([429224f](https://github.com/madgeek-arc/resource-catalogue/commit/429224fd6f20b696c78aa45dbf9d2580087693f4))
* set base image back to OpenJDK 21 ([ba2e7d1](https://github.com/madgeek-arc/resource-catalogue/commit/ba2e7d1ae4d983f5588f86f0ed8c7782d41d5073))
* sets specific maven version for java 21 ([bfdff04](https://github.com/madgeek-arc/resource-catalogue/commit/bfdff04e97202433e6634861a3ff685976f30521))
* token in headers when submitting a ticket ([de32231](https://github.com/madgeek-arc/resource-catalogue/commit/de322315e15e62f0e595fb846c9f194c0e9a1b2e))

## [5.5.0](https://github.com/madgeek-arc/resource-catalogue/compare/v5.4.0...v5.5.0) (2026-05-06)


### ⚠ BREAKING CHANGES

* rename deployable software to application
* rename Deployable Service to Deployable Software

### Features

* Adds new endpoint to retrieve vocabularies by type ([9516601](https://github.com/madgeek-arc/resource-catalogue/commit/9516601c7a1069baac58e2db2b97ddc4ed2bf4ca))
* **aop:** introduce trigger-based post-processing aspect for resource lifecycle actions ([6d3b4c6](https://github.com/madgeek-arc/resource-catalogue/commit/6d3b4c6f3a1253f187ae8a968b1b2226949dd5df))
* Create onboarding workflow for services / tranining resources ([c424ef4](https://github.com/madgeek-arc/resource-catalogue/commit/c424ef47a948d267dedfa2330dc80126f9c1c434))
* Creates vocabularies by type method with parent_id filtering ([43469b3](https://github.com/madgeek-arc/resource-catalogue/commit/43469b39761facf3992f8876f2665b1f3384f847))
* functionality to retrieve public services with highlights ([c80f6e9](https://github.com/madgeek-arc/resource-catalogue/commit/c80f6e9e7ac23aad743defe36b9e4e8dd9f9bbbe))
* Implements Flowable onboarding ([c7fcff4](https://github.com/madgeek-arc/resource-catalogue/commit/c7fcff45596a89cbdf4ff65cff360a7b4239cfb9))
* node registration on node registry during wizard setup ([d03f28e](https://github.com/madgeek-arc/resource-catalogue/commit/d03f28ee0d939b20ef73e2c9b0ce8f0aaa2ef1c4))
* **onboarding:** Adds Camunda dependencies, element templates, workflows and a basic onboarding service ([78f94dc](https://github.com/madgeek-arc/resource-catalogue/commit/78f94dc33e9b3d2f9d52ff14d88df754f3df5d72))
* **onboarding:** Creates a controller to test onboaring workflow functionality ([db9b59e](https://github.com/madgeek-arc/resource-catalogue/commit/db9b59e4c9cef6d1b4ef2eec25ef73371e49c8cd))
* Provider onboarding script and set audit status template ([f00d363](https://github.com/madgeek-arc/resource-catalogue/commit/f00d363d06eb513739e76783b7e859bce763e579))
* SQA assessment functionality on newly added adapters ([0b621a9](https://github.com/madgeek-arc/resource-catalogue/commit/0b621a9c4767838eff706bad6e92556d95f103f2))
* workflow service for adapters, guidelines ([cd06262](https://github.com/madgeek-arc/resource-catalogue/commit/cd062625dc9c4388409606d40208cbe836f599a3))


### Bug Fixes

* add case for Adapters on getProviderId() method ([419a832](https://github.com/madgeek-arc/resource-catalogue/commit/419a8326e7999720f311a399ba981d50e5170b45))
* add proper profile on AuthTokenService ([05a2502](https://github.com/madgeek-arc/resource-catalogue/commit/05a25025661879657259679cfd496794d23b6aeb))
* Adds missing datasource configuration ([5bbdea9](https://github.com/madgeek-arc/resource-catalogue/commit/5bbdea9f216b8694c79e75602fcec35b420dbfd3))
* align resource type JSON schemas with Everit draft-07 support ([72fee3b](https://github.com/madgeek-arc/resource-catalogue/commit/72fee3bbead4a1e98eeac6b3cc86ffe55d73191f))
* beforeBodyWrite() now returns null for null objects ([2bd76b7](https://github.com/madgeek-arc/resource-catalogue/commit/2bd76b700343920fc4a1c998f6e6754106309ee9))
* change 'version' label/name as it is reserved on elastic ([7c7b16b](https://github.com/madgeek-arc/resource-catalogue/commit/7c7b16bb33f192ec8e13d06df0c0523fd7653eba))
* check for Datasource ID inside the map during add ([218e482](https://github.com/madgeek-arc/resource-catalogue/commit/218e48290c7479dfc9b93c97f2c50513b3675824))
* circular dependency ([54b9e9b](https://github.com/madgeek-arc/resource-catalogue/commit/54b9e9b1255e1b1ce39e036c71895b87912ce2cf))
* circural dependency ([c38be65](https://github.com/madgeek-arc/resource-catalogue/commit/c38be65fd45102eb22be2c62c2fd052fa2411432))
* classpath for configuration template resource types ([aa72122](https://github.com/madgeek-arc/resource-catalogue/commit/aa7212230ed0c31a1fa259d082d34b58ea757f90))
* correct messages for JMS/AMS ([1f2cebd](https://github.com/madgeek-arc/resource-catalogue/commit/1f2cebd61d93a0b9d1adfb5a831b04720d004eee))
* Corrects configuration for separate db ([09164ca](https://github.com/madgeek-arc/resource-catalogue/commit/09164cab4bdf56e6dd928cfcd070f38b26ae29dd))
* Corrects typeInfo properties. ([7d5bb0d](https://github.com/madgeek-arc/resource-catalogue/commit/7d5bb0d799edcbe8e3ae9d563a66f6f5d610f157))
* Create identifiers before running workflow ([37c6c3a](https://github.com/madgeek-arc/resource-catalogue/commit/37c6c3a0183f479fc5e1674097fc590d4990925b))
* ct, cti resource type classpath properties ([d5c46b3](https://github.com/madgeek-arc/resource-catalogue/commit/d5c46b3df0e0d7cc40cb3a96fd9a74dd8b602577))
* Decreases timeout and increases active job count of workers ([c9c94c6](https://github.com/madgeek-arc/resource-catalogue/commit/c9c94c6f6f03fc6c86365ca3dd0d18b01f9c2acd))
* **deps:** Bumps camunda dependency version to fix issue where @JobWorker custom properties were ignored ([dc60207](https://github.com/madgeek-arc/resource-catalogue/commit/dc60207a29968bb9e20646fb8e49c57ef160bdc1))
* Draft finalize issue and refactoring ([dccb700](https://github.com/madgeek-arc/resource-catalogue/commit/dccb700d5ec9f8d4eace5d99fbb66332c054d1de))
* duplicate 'registered' onboarding logging info entry for draft resources ([a1cc1fa](https://github.com/madgeek-arc/resource-catalogue/commit/a1cc1faf44896cd346acd89634bdbbf4aa3d643c))
* Escapes & in the xml ([80f4d96](https://github.com/madgeek-arc/resource-catalogue/commit/80f4d96b98bc1981a75e123d6b0579c987550ff1))
* Failures in validation are not masked as RuntimeException from the aspect. ([4badd9a](https://github.com/madgeek-arc/resource-catalogue/commit/4badd9a6d7b37992c24b59c388624f65d3315625))
* finalize draft methods now pass through workflow service ([6154da9](https://github.com/madgeek-arc/resource-catalogue/commit/6154da959cc35b13f835b22c73d35a54afad0c46))
* finalize draft methods now pass through workflow service ([d2f7f7e](https://github.com/madgeek-arc/resource-catalogue/commit/d2f7f7eb9887c4d1d18573b3a2add2b8b3bd6ca7))
* Fix Onboard Provider workflow ([1c0ca9d](https://github.com/madgeek-arc/resource-catalogue/commit/1c0ca9da7d0204f68c6c7a7a67b1e9e5fd2f0b2a))
* logic on canAddResources() method ([2eeafd2](https://github.com/madgeek-arc/resource-catalogue/commit/2eeafd216c7fb7845105642dab64dc209f56c721))
* maven POM configurations for additional profile compatibility - lot1 ([1df4c29](https://github.com/madgeek-arc/resource-catalogue/commit/1df4c298281a28f90bae1f687befe0a9a95ba008))
* minor fix ([2f23873](https://github.com/madgeek-arc/resource-catalogue/commit/2f238735faa3a07c49230dd803523abbd55221ce))
* minor fix on getMy() method. Move on generic manager ([5d98c13](https://github.com/madgeek-arc/resource-catalogue/commit/5d98c1344c2c1ee0bb5f43c8abf9b3e5890cd715))
* minor fix on guideline onboarding bpmn ([50ae244](https://github.com/madgeek-arc/resource-catalogue/commit/50ae2446f9fe2e879aa97b3a2b586dd9e3fe4570))
* minor fix on RIR manager. Add Datasource as an option, removed Training ([73c1933](https://github.com/madgeek-arc/resource-catalogue/commit/73c19333620bbf73fe896835b17c81b4f0f55a02))
* minor routing fix ([dd9049f](https://github.com/madgeek-arc/resource-catalogue/commit/dd9049fccd448f9bcb1a224cad84e94aec09b2b4))
* onboard-resource wf ([db067f7](https://github.com/madgeek-arc/resource-catalogue/commit/db067f7494087d1a038ba8a36d1b8a1a57a1a23f))
* organisation on bpmn workflows ([9c542e4](https://github.com/madgeek-arc/resource-catalogue/commit/9c542e4c43cbe215cb31a5f33c5ca4f35f454a52))
* organisation on resource types and configuration files ([4cd08eb](https://github.com/madgeek-arc/resource-catalogue/commit/4cd08ebfbeb15fc3c50fe28d5cab983947be5d8e))
* path fixes on various resource types ([10ff9e7](https://github.com/madgeek-arc/resource-catalogue/commit/10ff9e7186f2c5f0a988feca290794207b5c00ef))
* Provider onboarding issue with missing templateStatus ([b5633a7](https://github.com/madgeek-arc/resource-catalogue/commit/b5633a7bbbc65afaad2e8dfa4abca6553ba89a75))
* public getAll method now returns only public resources ([fbd42e7](https://github.com/madgeek-arc/resource-catalogue/commit/fbd42e7b6fbcb47ba02240fe3cfd136466dde598))
* remove dependency patches that broke swagger ui ([b602a27](https://github.com/madgeek-arc/resource-catalogue/commit/b602a274628329a4225ceaad98a5e1715a1dad03))
* remove extra update while finalizing drafts, because the update was passing without triggering the validation. remove triggering aspect functionality for emails ([0566afb](https://github.com/madgeek-arc/resource-catalogue/commit/0566afb2b98bec228d4c3a682315c631de711da3))
* remove unnecessary suspension validation ([de1f36a](https://github.com/madgeek-arc/resource-catalogue/commit/de1f36ad74dd9288add8087d599c619e4b736fc0))
* Removes Camunda dependency and adds Flowable ([a9afb5f](https://github.com/madgeek-arc/resource-catalogue/commit/a9afb5f02bcb5dce7dc91672210a944910bebc24))
* Removes dependency to catalogue service causing circular dependency issue ([164f9c2](https://github.com/madgeek-arc/resource-catalogue/commit/164f9c2fafc3cf9306a6c3754628018c8bf6d58e))
* rename reserved keyword ([63d6c33](https://github.com/madgeek-arc/resource-catalogue/commit/63d6c3310ec6287233b97f80d2caf31004eba60f))
* Renames 'owner' to 'resourceOwner' in resource-onboard workflow. ([916d5e4](https://github.com/madgeek-arc/resource-catalogue/commit/916d5e4432b4c88702fd4869fc5c65e7055d4162))
* Renames deployable-software to deployable-application ([d0cd45c](https://github.com/madgeek-arc/resource-catalogue/commit/d0cd45ce91daa4fdc3a9e724d55f429bd134a39b))
* replace manager executions in various methods on ProviderManagementAspect ([c7192f9](https://github.com/madgeek-arc/resource-catalogue/commit/c7192f9be0a800a33b1690c90baf05e972f65264))
* Restores separate datasource configuration. Reuses main datasource's connection properties but in a different schema. ([e6c5213](https://github.com/madgeek-arc/resource-catalogue/commit/e6c5213a9bfe980a81fc7ea49baf46abf6f25209))
* Secures APIs ([c9c1f9f](https://github.com/madgeek-arc/resource-catalogue/commit/c9c1f9fe6097861e59c27b124f54a54cdf7196be))
* **security:** patch vulnerabilities in dependencies ([0a51e58](https://github.com/madgeek-arc/resource-catalogue/commit/0a51e58b47ba40b0236e80b9eb9ac6b7530053c5))
* **security:** patch vulnerabilities in dependencies ([f1903c9](https://github.com/madgeek-arc/resource-catalogue/commit/f1903c9e4bbccfd21c2ee13ae2d6a48d3928359b))
* **security:** patch vulnerabilities in dependencies ([69f74fb](https://github.com/madgeek-arc/resource-catalogue/commit/69f74fb11b6f9c0e2e840ed4d3fbdf8e68194600))
* **security:** patch vulnerabilities in dependencies ([73cc3df](https://github.com/madgeek-arc/resource-catalogue/commit/73cc3df669b89cf40ec7034714cea94fae1cf152))
* **security:** patch vulnerabilities in dependencies. Add owasp dataDirectory config for shared NVD cache ([f65282f](https://github.com/madgeek-arc/resource-catalogue/commit/f65282fdfd5bd817d96f583325d319b5ed3b7fb2))
* Serializes userInfo before passing to variables ([e434976](https://github.com/madgeek-arc/resource-catalogue/commit/e434976fd08652ba6ea039c3d9f5248ff6d445ec))
* Service onboard moved to bpmn workflow ([add1874](https://github.com/madgeek-arc/resource-catalogue/commit/add1874cda82f97ebca1881f0a929beaa0dc4a41))
* update catalogue version to fix validation issue ([5d93465](https://github.com/madgeek-arc/resource-catalogue/commit/5d93465c6acbaa916309e0385c80aac8109e1185))
* update catalogue version to fix validation issue ([e68d3ee](https://github.com/madgeek-arc/resource-catalogue/commit/e68d3ee662cb485c263c341279da566bb8f25929))
* updating public IDs for RIR and CTI now searches in the correct services ([4033e50](https://github.com/madgeek-arc/resource-catalogue/commit/4033e50acab52d486da4e3f7c6ff8f2a8e1aaf49))
* User roles mapping issue for non-admin users. ([c8fcd48](https://github.com/madgeek-arc/resource-catalogue/commit/c8fcd48b3f62f2c0d86d5d1a38f5d39b4475d771))
* Uses default catalogue if not provided ([3c8d4a8](https://github.com/madgeek-arc/resource-catalogue/commit/3c8d4a855b6ff5e4cf06d67860d4e37bc608b884))
* wizard step3 - adding main/default catalogue ([d90897e](https://github.com/madgeek-arc/resource-catalogue/commit/d90897e259a74b78238dd434237416c4deeef50e))


### Documentation

* README and configuration files ([6715463](https://github.com/madgeek-arc/resource-catalogue/commit/67154635104b3e36f37756fa6389dbab6a6e20b7))
* update README.md ([c874e50](https://github.com/madgeek-arc/resource-catalogue/commit/c874e50f437d0e61998e986ed11d0518a5e72363))


### Miscellaneous Chores

* release 5.5.0 ([c203481](https://github.com/madgeek-arc/resource-catalogue/commit/c203481c0ac8be062aea5c65745c49fa259d9745))


### Code Refactoring

* rename Deployable Service to Deployable Software ([52e4508](https://github.com/madgeek-arc/resource-catalogue/commit/52e450822d6f88cd5f56c3763869a0dafadb1768))
* rename deployable software to application ([7800cc1](https://github.com/madgeek-arc/resource-catalogue/commit/7800cc1aab6a21c8a72b8949ad029f3815edfc9e))

## [5.4.0](https://github.com/madgeek-arc/resource-catalogue/compare/v5.3.0...v5.4.0) (2025-12-23)


### Features

* creates method to retrieve user info ([87f1527](https://github.com/madgeek-arc/resource-catalogue/commit/87f15273a3045e7af2afb261c1807689d628a0c8))


### Bug Fixes

* creates AuthTokenService responsible of providing valid access tokens for the authenticated user ([c12bd63](https://github.com/madgeek-arc/resource-catalogue/commit/c12bd63c216c9abf67118a8bf90a10f74471f7f3))
* replaces deprecated annotation ([ad51d86](https://github.com/madgeek-arc/resource-catalogue/commit/ad51d86c8558f0487db943f6ed4e22c04bf92a1a))

## [5.2.0](https://github.com/madgeek-arc/resource-catalogue/compare/v5.1.0...v5.2.0) (2025-09-15)


### Bug Fixes

* created, updated fields on InteroperabilityRecord now get consistent values of 'yyyy-MM-dd' format on POST/PUT ([1d5b0c7](https://github.com/madgeek-arc/resource-catalogue/commit/1d5b0c7a5716216929f4939734c87dae76c80432))
* getDraftServices() now correctly returns draft services ([7500821](https://github.com/madgeek-arc/resource-catalogue/commit/7500821a3efe21362f30531a9c8c85d0bb3b4081))
* hide allRequestParams to fix OpenAPI codegen issue (MultiValueMapStringObject) ([09a634f](https://github.com/madgeek-arc/resource-catalogue/commit/09a634f566772d6484eb62e9271d9225e31f7670))
* hide allRequestParams to fix OpenAPI codegen issue (MultiValueMapStringObject) ([3d16df4](https://github.com/madgeek-arc/resource-catalogue/commit/3d16df4891ffa92d62cf93396345444714eb7274))
* minor fixes in broken email templates. rename of portal -&gt; beyond. enable Openaire Datasource registration ([b7331a0](https://github.com/madgeek-arc/resource-catalogue/commit/b7331a0dd975d1a02e661f3827810e1ce38e05f6))
* re-introduce deleted method ([726445c](https://github.com/madgeek-arc/resource-catalogue/commit/726445ce66d99c364d20fe47aa358ea257109010))
* remove 'Beyond' word from emails as it is problematic for other projects/catalogues ([d7c7bcb](https://github.com/madgeek-arc/resource-catalogue/commit/d7c7bcb7bb721f4650e2f5bda88ebe697e288dce))
* remove 'Beyond' word from emails as it is problematic for other projects/catalogues ([9bd56bd](https://github.com/madgeek-arc/resource-catalogue/commit/9bd56bd0a812461312d219ba1c20427098b2395c))
* replace hard-coded version with var ([44c03b8](https://github.com/madgeek-arc/resource-catalogue/commit/44c03b8430467338d434ef531d85e2066b6d4447))
* skip Matomo update when host is not configured and warn on failure ([ab84d54](https://github.com/madgeek-arc/resource-catalogue/commit/ab84d549e3170cf24a3fa772dee27cd98b45fb4a))
* update on public adapter resource ([a802488](https://github.com/madgeek-arc/resource-catalogue/commit/a8024885cd1525075e41d318b88a7a3c4b91415b))
* update on public adapter resource ([d38b635](https://github.com/madgeek-arc/resource-catalogue/commit/d38b635d257c424f6b0fcd0018e77ab6f1ffc68d))
* **verify:** reintroduce method ([1cfa30f](https://github.com/madgeek-arc/resource-catalogue/commit/1cfa30fc351fffa967c95d3852ae6cfdc9d07a0f))

## [5.1.0](https://github.com/madgeek-arc/resource-catalogue/compare/v5.0.1...v5.1.0) (2025-07-28)

### Features

* adapter public service ([1ddc28b](https://github.com/madgeek-arc/resource-catalogue/commit/1ddc28bc3e8ec0dc2fcb6bfc0def4ba1a26569e6))
* adapter public service ([a01c75a](https://github.com/madgeek-arc/resource-catalogue/commit/a01c75a6ba44a1c37caeb77bc8bda99c9a797889))
* adapters - initial commit ([54586df](https://github.com/madgeek-arc/resource-catalogue/commit/54586dfbfbfa7164553501816bc5f076f30a8392))
* Add enable/disable functionality for AMS service ([5510c6a](https://github.com/madgeek-arc/resource-catalogue/commit/5510c6a1beb20a78b89de4d31694d77368891426))
* add scaffold for adapter functionality ([9bfb102](https://github.com/madgeek-arc/resource-catalogue/commit/9bfb102355c7b2c88258f4ade34582b60e9d92d9))
* auto-assign EOSC Monitoring IG on Service onboard (approved status) ([8dfc3b1](https://github.com/madgeek-arc/resource-catalogue/commit/8dfc3b1abe630f648d52e4186bcea0343c69eef3))
* configuration template initial commit ([0a892db](https://github.com/madgeek-arc/resource-catalogue/commit/0a892dba057efbeb85b3dc3bec8f0fc54914fc23))
* deployable service functionality ([6c21446](https://github.com/madgeek-arc/resource-catalogue/commit/6c214466826571c4432d6b60c4abfc6e7d25ea32))
* deployable service initial commit ([7326fd0](https://github.com/madgeek-arc/resource-catalogue/commit/7326fd0585671a916f199785d4e6537382a3bde6))
* implement basic adapter methods ([e3559d9](https://github.com/madgeek-arc/resource-catalogue/commit/e3559d9232f16375f1feff0cfb1afdc6a789c23b))
* sorting on linkedResource dropwdown related front-end calls ([85d40ec](https://github.com/madgeek-arc/resource-catalogue/commit/85d40ec300e4782276ef0de8173bb36528c49c63))

### Bug Fixes

* add prefix on resource-catalogue JMS messages ([0f9177c](https://github.com/madgeek-arc/resource-catalogue/commit/0f9177c53c7cab025c5969d6600ee41f93acd1e6))
* Add profile 'no-auth' when running tests and add missing properties ([c5a361c](https://github.com/madgeek-arc/resource-catalogue/commit/c5a361c0e53e1ce314eba9baefecc6bf7eb39362))
* add public CTI and Adapter fix ([cb05a92](https://github.com/madgeek-arc/resource-catalogue/commit/cb05a922515477fa5d8692682b773139a43e6214))
* add ROLE_USER on token authentication ([e24a69f](https://github.com/madgeek-arc/resource-catalogue/commit/e24a69fd9c2587ab3f0a5b9f2441400b28836a36))
* addAdapterAsPublic() now tries to fetch the correct public level adapter resource ([15a839e](https://github.com/madgeek-arc/resource-catalogue/commit/15a839e07a6e89ade24ef9e50b1a998438b54daf))
* adminAcceptedTerms catches the correct exception ([9cbe1f2](https://github.com/madgeek-arc/resource-catalogue/commit/9cbe1f287d388925362c5d50aa5d83e783060d3e))
* adminAcceptedTerms now catches the correct exception ([b2e9824](https://github.com/madgeek-arc/resource-catalogue/commit/b2e9824d4d8acab350b83eb2c12866a0294a7661))
* correct Bundle property class on configuration template related .json files ([7c54f80](https://github.com/madgeek-arc/resource-catalogue/commit/7c54f80141a26bb82eb56f8f8c09311875ad02d0))
* deleting a RIR or updating its Guidelines' list should now properly delete any associated CTI ([8a6b535](https://github.com/madgeek-arc/resource-catalogue/commit/8a6b53572e1857e000dbb70dcbaa7094da8d7f85))
* deletion on public CT, CTIs ([86ff8b1](https://github.com/madgeek-arc/resource-catalogue/commit/86ff8b199d1a4305467fd5e0dd3c41091afba2bc))
* dynamic property reload for admin/epot list ([b13ce2e](https://github.com/madgeek-arc/resource-catalogue/commit/b13ce2e8b9077b70c079af362b07588d178227f1))
* dynamic property reloading for admins/epots (requires relog) ([95f78e8](https://github.com/madgeek-arc/resource-catalogue/commit/95f78e801bef4486d5086376874f4231253d1bb1))
* fetch correct vocabulary state for deployable services during verify ([5feda7c](https://github.com/madgeek-arc/resource-catalogue/commit/5feda7cb2682c2361c12451242d9d61d1d3ebd18))
* Filter out unecessary configuration files when profile 'no-auth' is enabled ([c5a43aa](https://github.com/madgeek-arc/resource-catalogue/commit/c5a43aaef7f10f5b65f92281a1261134d28aee1f))
* minor fix on ResourceValidationUtils ([be3d03a](https://github.com/madgeek-arc/resource-catalogue/commit/be3d03a124b87187f964d4c13211029e1e2d0269))
* node vocabulary enum type name ([9510b3c](https://github.com/madgeek-arc/resource-catalogue/commit/9510b3c16f8423e37cf165cb0793e0680a7c9971))
* populate getCatalogueId() method with the new resources ([59f9afb](https://github.com/madgeek-arc/resource-catalogue/commit/59f9afb429cc41f89e15661db5a3f3e75af8d0f7))
* remove problematic annotations ([4adefc7](https://github.com/madgeek-arc/resource-catalogue/commit/4adefc75e9014e8e01996ea65ef50e583a990421))
* required fields on Service, Datasource ([918e88a](https://github.com/madgeek-arc/resource-catalogue/commit/918e88a4b8389f97bfc89eeb4cad8d5224a72b20))
* update methods on various resources now correctly update lower-level resources ([05ce307](https://github.com/madgeek-arc/resource-catalogue/commit/05ce30768cd8accc8fc2f998509c92e896757e04))
* updateIdsToPublic() method checks for existance first where needed ([acf562c](https://github.com/madgeek-arc/resource-catalogue/commit/acf562c49d6d596bb3180ba3a55acb736cabaa59))
* use getResource() with published and catalogueId ([91e5052](https://github.com/madgeek-arc/resource-catalogue/commit/91e50522dbe1d8c433ed13340cf61692404ab32f))
* validation on Adapter's linked resource field. ([21dca9c](https://github.com/madgeek-arc/resource-catalogue/commit/21dca9c5f77e69addaa130e0fafd3873ab4453b9))


## [5.0.1](https://github.com/madgeek-arc/resource-catalogue/compare/v5.0.0...v5.0.1) (2025-05-22)

### Bug Fixes

* add prefix on resource-catalogue JMS messages ([4234b15](https://github.com/madgeek-arc/resource-catalogue/commit/4234b1537a78697861bb1ff886ff156d7af72d51))
* Bump dependency versions to fix ControllerAdvice issue when loading swagger ([e0b4d6e](https://github.com/madgeek-arc/resource-catalogue/commit/e0b4d6e338459c4044ff069c27500d44d8ca53e4))

## [5.0.0](https://github.com/madgeek-arc/resource-catalogue/compare/v4.1.2...v5.0.0) (2025-05-21)

### ⚠ BREAKING CHANGES

* Change "catalogue" dependency FQPN

### Features

* Add dependency on 'catalogue' project. Remove duplicate code. ([c780de1](https://github.com/madgeek-arc/resource-catalogue/commit/c780de1fb7d063076eb20da7edf02d2e25baf446))
* Add enable/disable functionality for AMS service ([a79b2ed](https://github.com/madgeek-arc/resource-catalogue/commit/a79b2edb9afc2d4cc1539e492d72b30b1928b3a4))
* AMS initial commit ([923faa2](https://github.com/madgeek-arc/resource-catalogue/commit/923faa23b28495e09ea9aa13b4399ee07ea9de80))
* AMS initial commit ([626a8a3](https://github.com/madgeek-arc/resource-catalogue/commit/626a8a3dba7931f6eff0a818bdb41471e3f2e1e9))
* Create new API call to get a Configuration Template providing its Interoperability Record ID ([4bc8fc2](https://github.com/madgeek-arc/resource-catalogue/commit/4bc8fc2ed7d2358334c1cc1d0b809a917b850317))
* Support multiple resolve endpoints instead of a single one (old marketplaceEndpoint), during pid JSON creation ([b47e068](https://github.com/madgeek-arc/resource-catalogue/commit/b47e068049c2dc352c5cc3e9c653e982798c3741))

### Bug Fixes

* Add https prefix for docker registry ([8c49f2d](https://github.com/madgeek-arc/resource-catalogue/commit/8c49f2d31d778a44e138c00a5e86ff41e039ee90))
* Add https prefix for docker registry ([b4d35d7](https://github.com/madgeek-arc/resource-catalogue/commit/b4d35d737cbfdafd0cc29e5c88c20fb0582b2cc9))
* Add missing dependency ([9e5b69b](https://github.com/madgeek-arc/resource-catalogue/commit/9e5b69b89005c4c749c7fc89a6d4f8cfa63545af))
* Add needed missing action (post/put) on Monitoring email template ([2e01b53](https://github.com/madgeek-arc/resource-catalogue/commit/2e01b537a0d5e9194e06b7b7ba72481c28522b14))
* Add missing email field from user info cookie ([82db52f](https://github.com/madgeek-arc/resource-catalogue/commit/82db52fca428132e1abc7dee72668ac90fe7f89e))
* Bump httpclient5 version ([ca2f1c9](https://github.com/madgeek-arc/resource-catalogue/commit/ca2f1c98d6a48b2f8e9bba3d78a3910096815fe4))
* Change docker login method and image push ([1ffb56c](https://github.com/madgeek-arc/resource-catalogue/commit/1ffb56cfd800ee92865a2928159fabb9f2b5e482))
* Correct public ID creation for resource interoperability records when sending consistency emails ([9e5586c](https://github.com/madgeek-arc/resource-catalogue/commit/9e5586c2b2ec76f39b69f7b2de5867dbc720faf9))
* Correct consumes and produces on methods and created method to get resources using pid ([36a0e67](https://github.com/madgeek-arc/resource-catalogue/commit/36a0e67ba431d8827c4c2356fdfdf2afaaa7ae08))
* Fix problematic usage of FacetFilter on getMy() methods ([0c0abad](https://github.com/madgeek-arc/resource-catalogue/commit/0c0abad9270b8f7ba3733fc5c203cb6beb9a6c36))
* Get user info using token for 'eosc' oidc provider to extract email and assign user roles ([5bdd941](https://github.com/madgeek-arc/resource-catalogue/commit/5bdd941013b09b9069c114a681fdb22a54076112))
* Modify request matchers in filter chain ([e06dc4a](https://github.com/madgeek-arc/resource-catalogue/commit/e06dc4a3a2c9f2e7274ec1d2536f0e5a78039866))
* Remove null emails if found in user object ([ee46192](https://github.com/madgeek-arc/resource-catalogue/commit/ee46192b59e648703ea874e1d5c59d7c73f3b52e))
* Replace misleading HttpStatus code ([79ee3cf](https://github.com/madgeek-arc/resource-catalogue/commit/79ee3cfbf44b14b894c8fa54d461570900bd5f56))
* Restore log formatting ([3469a67](https://github.com/madgeek-arc/resource-catalogue/commit/3469a676b5a64aa9f1fc1f8b2530f2db0db0dd95))
* serviceTypeValidation() now keeps a list of service type ids instead of names ([08bd43e](https://github.com/madgeek-arc/resource-catalogue/commit/08bd43e75c79e0e130356f773fcc9dfd03adfd73))

### Reverts

* Revert "build: change dependencies and versions" ([f41236d](https://github.com/madgeek-arc/resource-catalogue/commit/f41236d462b59838d39325edab381ecc439249bf))
* Revert "Jaxb2 marshalling" ([f907ab3](https://github.com/madgeek-arc/resource-catalogue/commit/f907ab3855aaf686ed2816cc8563c04c6f6e809b))
* Revert "updated registry core version" ([f1ff86d](https://github.com/madgeek-arc/resource-catalogue/commit/f1ff86d38d0b7422aef37facda61246f8d950646))

### Build System

* Change "catalogue" dependency FQPN ([48dce34](https://github.com/madgeek-arc/resource-catalogue/commit/48dce34d15beb5d40458e2388c0bc3a3fb2af3b7))

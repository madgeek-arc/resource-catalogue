## [](https://github.com/madgeek-arc/resource-catalogue/compare/v5.0.0...v) (2025-05-22)

### Bug Fixes

* add prefix on resource-catalogue JMS messages ([4234b15](https://github.com/madgeek-arc/resource-catalogue/commit/4234b1537a78697861bb1ff886ff156d7af72d51))
* Bump dependency versions to fix ControllerAdvice issue when loading swagger ([e0b4d6e](https://github.com/madgeek-arc/resource-catalogue/commit/e0b4d6e338459c4044ff069c27500d44d8ca53e4))
## [](https://github.com/madgeek-arc/resource-catalogue/compare/v3.0.1...v) (2025-05-21)

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

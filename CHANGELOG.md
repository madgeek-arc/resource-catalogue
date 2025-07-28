## [](https://github.com/madgeek-arc/resource-catalogue/compare/v5.0.1...v) (2025-07-28)

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

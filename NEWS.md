## 15.6.2 2020-03-12
* Fix PostgreSQL connection memory leak by updating RMB to 27.1.2. Refs MODFEE-26, RMB-485.

## 15.6.1 2020-02-17
* Fix json-schema for lost item fee policy. Refs UIU-1156.

## 15.6.0 2019-09-08
* Update 26.2.4 RMB version. Refs Upgrading Version 25.
* Add patron notice for payment of fee/fine charge. Refs MODFEE-11.
* Add patron notice for manual fee/fine charge. Refs MODFEE-12.
* Fix POST to resources. Fixes MODFEE-2, UIU-1150.
* Add fee/fine tokens to patron notices. Refs MODFEE-13.
* CRUD Fee/Fine Overdue Fine Policies. Refs UIU-1146.
* CRUD Fee/Fine Lost Item Fee Policies. Refs UIU-1156.

## 15.5.0 2019-08-06
 * Fix unclosed transaction. Refs MODFEE-10.

## 15.4.0 2019-06-05
 * Modify resource waives.
 * Upgrade to RMB 24.0.0.

## 15.3.0 2019-04-02
 * Upgrade to RMB 23.7.0.
 * Fix code smell.
 * Modify resources owners an feefines.Refs UIU-713.

## 15.2.1 2019-02-27
 * Add fields defaultChargeNotice and defaultActionNotice for resource owners. Refs UIU-713.
 * Add fields chargeNotice and actionNotice for resource feefines. Refs UIU-713.
 * Upgrade to RMB 23.6.0.

## 15.2.0 2019-02-19
 * Upgrade to RMB 23.5.0.
 * Modify resource transfers and add transfer-criterias resource. Refs UIU-544.
 * Fix validation. Fixes MODFEE-3, MODFEE-4, MODFEE-5, MODFEE-6, MODFEE-7

## 15.1.0 2018-12-10

 * Modify with Metadata (commit b682da5537b1fb35301156b6e48d66385a5828f1)
 * Remove id required. Fix to MODFEE-2.
 * Remove feesfines all permission (commit d883537a3b112ee8d84c617d9a8d25c89380c6b8)
 * FOLIO-1609: Disable system class loader in surefire and failsafe plugins

## 15.0.1 2018-09-25
 * Upgrade to RMB 23.0.0, vertx 3.5.4.
 * Modify resource accounts.
 * Update interface version descriptor.
## 15.0.0 2018-10-18
 * Add field servicePointOwner for resource owners. Refs UIU-611.
 * Modify resource feefineactions. Refs UIU-646.
 * Modify resource payments. Refs UIU-612.
 * Add field ownerId as required for resource feefines. Fix to MODFEE-8.
 * Modify resource feefines. Refs UIU-610.
 * Upgrade to RMB 21.0.4, use RAML 1.0.
 * Update shared raml-util.
 * Add new resource manualblocks. Refs UIU-674.
## 14.2.4 2018-09-11
 * Provide the management of the manual fees and fines.

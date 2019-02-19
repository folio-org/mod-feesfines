## 15.2.1 UNRELEASED
 * Add fields defaultChargeNotice and defaultActionNotice for resource owners. Ref UIU-713.
 * Add fields chargeNotice and actionNotice for resource feefines. Ref UIU-713.
 * Upgrade to RMB 23.6.0.

## 15.2.0 2019-02-19
 * Upgrade to RMB 23.5.0.
 * Modify resource transfers and add transfer-criterias resource. Refs UIU-544.
 * Fix validation. Fixes MODFEE-3, MODFEE-4, MODFEE-5, MODFEE-6, MODFEE-7

## 15.1.0 2018-12-10

 * Modify with Metdata (commit b682da5537b1fb35301156b6e48d66385a5828f1)
 * Remove id required. Fix to MODFEE-2.
 * Remove feesfines all permission (commit d883537a3b112ee8d84c617d9a8d25c89380c6b8)
 * FOLIO-1609: Disable system class loader in surefire and failsafe plugins

## 15.0.1 2018-09-25
 * Upgrade to RMB 23.0.0, vertx 3.5.4.
 * Modify resource accounts.
 * Update interface version descriptor.
## 15.0.0 2018-10-18
 * Add field servicePointOwner for resource owners. Ref UIU-611.
 * Modify resource feefineactions. Ref UIU-646.
 * Modify resource payments. Ref UIU-612.
 * Add field ownerId as required for resource feefines. Fix to MODFEE-8.
 * Modify resource feefines. Ref UIU-610.
 * Upgrade to RMB 21.0.4, use RAML 1.0.
 * Update shared raml-util.
 * Add new resource manualblocks. Ref UIU-674.
## 14.2.4 2018-09-11
 * Provide the management of the manual fees and fines.

## 18.0.2 2022-09-09
* Improve performance of Financial Transactions Detail Report (MODFEE-264)

## 18.0.1 2022-08-09
* Add support for different loan date formats to Financial Transactions Report (MODFEE-266)

## 18.0.0 2022-06-27
* Make `paymentStatus` property in accountdata.json required and also make it an enum (MODFEE-220)
* Prevent deleting/updating required `Feefine` (MODFEE-243)
* Reformat code according to FOLIO code style (MODFEE-244)
* Add missing account payment status `Suspended claim returned` (MODFEE-245)
* Return remaining amount in action response (MODFEE-247)
* Fix Refund report for cases when there are refunds linked to same account both inside and outside of the requested interval (MODFEE-249)
* Make comments field not required for cancelling a fee/fine (MODFEE-250)
* Validate that Overdue fine policy interval is not negative (MODFEE-252)
* Validate that Lost item processing fee value is not negative (MODFEE-253)
* Update tests with JUnit 5 (MODFEE-254)
* Add policy fields to the account record (MODFEE-257)
* Upgrade RMB to 34.0.0 (MODFEE-261)

## 17.1.0 2022-02-22
Reopen a fee/fine on refund (MODFEE-229)
Upgrade to RMB 33.0.4 and Log4j 2.16.0 (MODFEE-232)
Upgrade to RMB 33.2.4 (MODFEE-236)

## 17.0.0 2021-09-29
* Use BigDecimal to represent monetary values (MODFEE-29)
* Add schema-level validation for all UUIDs (MODFEE-98)
* Create endpoint to return data for Financial Transactions Detail Report (MODFEE-179)
* Extend fee/fine record with contributors info (MODFEE-198)
* Log patron notice processing errors to Circulation Log (MODFEE-200)
* Fix incorrect Due Date in Financial Transactions Report (MODFEE-201)
* Fix missing staff comment in Circulation Log records (MODFEE-202)
* Add refunded amount to fee/fine balance (MODFEE-203)
* Capture user details in Circulation Log record created upon manual block deletion (MODFEE-204)
* Fix incorrect date formatting in Financial Transactions Report (MODFEE-206)
* Fix missing `type` property in JSON-schemas for fields representing monetary values (MODFEE-210)

## 16.1.0 2021-06-14
* Refund lost item processing fees transferred to more than one account to their corresponding transfer accounts (MODFEE-138)
* Fix performance issues with large dataset of fees/fines (MODFEE-175)
* Create endpoint to return data for cash drawer reconciliation report (MODFEE-178)
* Fix remaining amount calculation for refund action (MODFEE-182)
* Fix search/filter on fees/fines not pulling recent circulation log actions (MODFEE-187)
* Create endpoint to fetch sources associated with service point (MODFEE-188)  
* Upgrade to RMB 33.0.0 and Vertx 4.1.0 (MODFEE-190)
* Upgrade to mod-pubsub-client 2.3.0 to fix error during registration in mod-pubsub (MODFEE-195)

## 16.0.0 2021-03-10
* Upgrade to RMB 32.1.0 and Vertx 4.0.0 (MODFEE-163)
* Add missing indexes (MODFEE-169)
* Make start date and end date not required (MODFEE-173)
* Add sorting fee/fine actions by date (MODFEE-170)
* Add Fee/fine owner as criteria for 'Refunds to process manually report' (MODFEE-160)
* Update account dateUpdated field on fees/fines actions (MODFEE-150)
* Code style changes (MODFEE-165)
* Fix failing of refund report when automatic refunds exist (MODFEE-164)
* Add fee/fine owner column to refund report, change paid/transferred columns logic (MODFEE-152)
* Return list of fee/fine actions created as a result of pay/waive/transfer/cancel/refund calls (MODFEE-147)
* Add an endpoint for building refund reports (MODFEE-144)
* Add property code to manual blocks (MODFEE-145)
* Upgrade to RMB 31.1.5 and Vertx 3.9.4 (MODFEE-140)
* Add indexes for tables `accounts` and `feefineactions` (MODFEE-141)
* Fix amount division logic for bulk paying/waiving/transferring (MODFEE-136)
* Add ability to provide custom cancellation reason for an account (MODFEE-137)
* Fix missing item details in fee/fine records in circulation log (MODFEE-139)
* Create manual-block template endpoint (MODFEE-133)
* Add UUID fields validation against the UUID regex pattern (MODFEE-88)
* Add Lost item fee (actual cost) type (MODFEE-114)
* Fix Fee/fine actions not appearing in circulation log (MODFEE-131)
* Fix PubSub permissions (MODFEE-124)
* Store new aged to lost settings for recalled items (MODFEE-146)
* Grants permission for messages to be published to pub-sub when deleting an account (MODFEE-151)

## 15.9.0 2020-10-14
* Add loanId to the FEE_FINE_BALANCE_CHANGED event payload (MODFEE-71)
* Extend/rename tokens available to manual fee/fine notices (MODFEE-72)
* Fix incorrect formatting of monetary values in patron notices (MODFEE-87)
* Fix missing permissions for fee/fine actions endpoint (MODFEE-89)
* Fix memory leak (MODFEE-93)
* Upgrade to RMB v31 and JDK 11 (MODFEE-103)
* Publish log events related to fees/fines (MODFEE-124)
* Publish log events related to manual blocks (MODFEE-125)
* Fee/fine actions refactoring (MODFEE-79, MODFEE-80, MODFEE-81, MODFEE-82, MODFEE-83, MODFEE-84, MODFEE-85, MODFEE-86, MODFEE-105, MODFEE-106, MODFEE-107, MODFEE-108, MODFEE-109, MODFEE-110, MODFEE-111, MODFEE-112, MODFEE-113)

## 15.8.0 2020-06-12
* Enable user-related tokens for manual fee/fine patron notices (MODFEE-66)
* Update RMB version to 30.0.1 and Vertx to 3.9 (MODFEE-59, MODFEE-62)
* Publish events when account is created, changed or deleted (MODFEE-53, MODFEE-54, MODFEE-63)
* Remove readonly fields validation (MODFEE-50)

## 15.7.0 2020-03-13
* Fix missing error message when duplicate Overdue Fine Policy name entered (MODFEE-16)
* Fix missing error message when duplicate Lost Item Fee Policy name entered (MODFEE-17)
* Send patron notice for waiver of fee/fine charge (MODFEE-19)
* Send patron notice for transfer of fee/fine charge (MODFEE-20)
* Send patron notice for cancellation of fee/fine charge (MODFEE-21)
* Fix sending "New fee/fine" and "Payment" notices when "Notify patron" is unchecked (MODFEE-22)
* Create default "out-of-the-box" Overdue Fine and Lost Item Fee policies (MODFEE-23)
* Fix incorrect next release version (MODFEE-24)
* Update tenant API version to 1.2 to allow reference data loading (MODFEE-25)
* Create automatic FeeFine records on module initialization (MODFEE-28)
* Fix error when saving Overdue Fine Policy (MODFEE-34)
* Add LaunchDescriptor settings (FOLIO-2234)
* Enable kube-deploy on Jenkins pipeline (FOLIO-2256)
* Remove old ModuleDescriptor "metadata" section (FOLIO-2321)
* Use JVM features (UseContainerSupport, MaxRAMPercentage) to manage container memory (FOLIO-2358)
* Fix lost item fee policy JSON-schema (UIU-1156)

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

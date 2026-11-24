# Currency Converter Pro · sandbox Android demo

The existing Java currency converter uses live rates from ExchangeRate-API. The debug app adds a RevenueCat Test Store purchase that unlocks one favourite currency pair. Saving a new pair replaces the previous one.

## RevenueCat setup

1. Create or select a RevenueCat project with a [Test Store](https://www.revenuecat.com/docs/test-and-launch/sandbox/test-store).
2. In **Product catalog**, create one Test Store one-time product with lifetime access (for example, `converter_pro_lifetime`). Create an entitlement with the identifier `pro` and attach the product to it.
3. Add the product to the **Lifetime** package in a single offering and make it the default offering. Make sure sandbox purchases can grant entitlements under **Sandbox Testing Access**.
4. Copy the public Test Store SDK key from **Project Settings → API keys**. Add it to the project's ignored `local.properties` file alongside any existing `sdk.dir` line.

   ```properties
   revenuecatTestStoreKey=test_your_key_here
   ```

## Build and run

Use JDK 17 and Android SDK Platform 35. Open the project in Android Studio and run the debug variant on an internet-connected emulator or device running Android 6.0 (API 23) or later. From a terminal, run `./gradlew assembleDebug` (`.\gradlew.bat assembleDebug` on Windows). The APK is at `app/build/outputs/apk/debug/app-debug.apk`. Without a key, the converter works and the purchase button is disabled.

If your test network blocks `api.revenuecat.com`, add `revenuecatTestStoreUseBackup=true` to `local.properties` and rebuild to use [RevenueCat’s backup endpoint](https://www.revenuecat.com/docs/getting-started/configuring-sdk#proxies--configuration-for-users-in-mainland-china-russia-and-myanmar).

## Try the three outcomes

Start with a fresh test install and select two currencies. Convert an amount, then scroll down to **Buy Currency Converter Pro** for each Test Store outcome in this order.

1. **Cancel**. Confirm the app reports cancellation, favourite controls stay hidden, and conversion still works.
2. **Test failed purchase**. Confirm the app reports failure, favourite controls stay hidden, and conversion still works.
3. **Test valid purchase**. Save the selected pair, change both selections, then tap the saved pair to reapply it. Force-stop and relaunch the same install, then confirm Pro unlocks and the saved pair can still be used.

**SDK observation.** On a fresh Android 11 test install, Logcat showed `PurchaseCancelledError` for Cancel and `TestStoreSimulatedPurchaseError` for the simulated failure. Both arrived through `PurchaseCallback.onError`, and the screen kept Pro locked in both cases.

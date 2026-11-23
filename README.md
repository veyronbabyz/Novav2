# Nova AI — real AI Android starter

This project connects the Android chat app to Google's Gemini Developer API using the `gemini-2.5-flash-lite` model.

## Features
- Real Gemini-generated answers over the internet
- Dark purple chat UI
- Conversation context during a chat
- Chat history stored locally
- New chat button
- API key setup screen (key is not embedded in the APK)

## Free API setup
1. Have a parent/guardian help review the provider's account and privacy terms if needed.
2. Open Google AI Studio: https://aistudio.google.com/app/apikey
3. Create a Gemini API key if your account is eligible.
4. Install Nova AI, tap **Settings**, and paste the key.
5. Connect to the internet and send a message.

Google's free tier has model availability and rate limits that can change. Check current terms and pricing at https://ai.google.dev/gemini-api/docs/pricing. On the free tier, Google states submitted content may be used to improve its products. Do not send passwords, private personal details, school records, or other sensitive information.

## API key safety
The key is saved in the app's private preferences on that phone and is not hardcoded in this source. This is suitable for personal testing only. Do not distribute a public APK that asks everyone to use your personal key or bundle a shared key in the APK. A public multi-user app should call a secured backend with authentication, quotas, abuse protection, and privacy disclosures.

## Build a free test APK from a phone using GitHub Actions
1. Create a GitHub repository.
2. Upload the contents of this folder, including the hidden `.github` folder.
3. Open the repository's **Actions** tab.
4. Run **Build Nova AI APK**.
5. When it finishes, download the `NovaAI-real-debug-apk` artifact.
6. Extract the downloaded ZIP and install `app-debug.apk` on your Android phone.

This is a debug APK for testing, not a Play Store release. For public release, create a signed release build and follow Google Play's current requirements.

## Local build
Requires JDK 17, Gradle 8.9+, Android SDK Platform 35 and Build Tools 35:
`gradle assembleDebug`

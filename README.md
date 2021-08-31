# LightsUp

## What is it?
LightsUp is a navigation application that allows you the see the streetlights in your area so you can take the shortest well-lit path to your destination when it's dark. 
LightsUp includes a feature to you to view logs from other uses describing which areas they did not feel safe in. You can also post about unsafe areas to warn others. We also provide helpful resources to learn more about the app, the problem and other informative resources.

[Figma Prototype](https://www.figma.com/proto/Ch9OqnMvcnXi7e7OHTpATT/LightsUp?node-id=22%3A4&scaling=scale-down&page-id=22%3A2&starting-point-node-id=22%3A4)

**DEMO**

![demo](https://user-images.githubusercontent.com/58123610/131246048-b376cc1c-04fa-4c9d-bdb5-b6705abf58a8.gif)

## Why we made it
Our app idea was inspired by our own past experiences of feeling uncomfortable/unsure of travelling alone at night time.

We want women and minority communities to feel safe while walking in the evening especially when the pandemic has led to an increase of race-based violence. We were motivated to create an app to address and communicate this issue of gender and race based violence.

In 2014, statistics showed that only 38% of women felt safe walking in their neighbourhood at night, vs the 64% of men. Sadly this experience is not limited to women. Members of marginalized communities such as LBGTQ+ members and racial minorities, feel an excess of anxiety when travelling at night. The increase of hate crimes during the pandemic has only boosted this anxiety. LightsUp strives to give these individuals more confidence when traveling and to amplify the discussion of violence towards these communities. Evening walks can also be difficult for the visually impaired which makes LightsUp vital for them.

This app is also helpful for tourists and newcomers who are unfamiliar with the area as it lets them know which routes are well-lit and which routes to avoid.

With rising summer temperatures walking outside during the day has become increasingly challenging so evening walks are preferred. By making travelling safer, LightsUp encourages people to try alternate modes of transportation (walking, biking, bussing etc.) as opposed to driving, which can help us reach our climate goals.

Covid-19 has increased the number of walks people take so doing it safely is a priority. The pandemic has also caused a rise in mental illness. LightsUp helps improve mental wellbeing while giving users peace of mind while exercising and encouraging them to spend time outside.

LightsUp aims to make outdoor activities accessible for everyone. Our app addresses global issues from helping the environment (encouraging alternate modes of transportation), gender equality, empowerment for minority groups and the visually impaired (helping women, minority groups and the visually impaired feel confident while walking outside) and mental well-being (encouraging outdoor activity for mental health).

## Technologies used
- Android SDK
- Mapbox Maps and Navigation SDKs

## To run the project
- Install the latest version of Android Studio from [here](https://developer.android.com/studio).
- Clone this repository
- Follow the steps [here](https://docs.mapbox.com/android/maps/guides/install/#configure-credentials) to create a Mapbox secret token
    - Add the following line in `gradle.properties`: `MAPBOX_DOWNLOADS_TOKEN=PASTE_YOUR_SECRET_TOKEN_HERE`

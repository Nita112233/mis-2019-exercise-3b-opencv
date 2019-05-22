# mis-2019-exercise-3b-opencv

Nita Kinanti - 120793

## 3b: Bonus (2 points)

**Add a README in which you briefly describe the issues you faced and how you resolved them.**

The first problem I found is to find the right tutorial in the internet. I used this [tutorial](https://medium.com/@kashafahmed/a-proper-beginners-guide-to-installing-opencv-android-in-android-studio-updated-5fe7f3399e1c) to integrate OpenCV in Android Studio and encountered few problems while implementing it. Most of the error came when I tried to sync the gradle, like when I mistakenly edited the wrong build.gradle file. 

## 3b: “Red Nose Day” (10 points)

**Add a README in which you briefly describe how your app determines the correct size for the red circle.**

I used haarcascade_mcs_nose.xml to detect the nose directly. The radius of the circle is determined by dividing the lenght of the nose rectangle by three to create a fitting clown nose effect. Since the "detectMultiScale" running continuously, the calculation for circle's radius will also changed as well in real time.

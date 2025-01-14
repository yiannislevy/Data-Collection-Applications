# Data Collection Applications

A suite of Android + Wear OS applications for real-time inertial, audio and weight data capture. These tools were developed as part of a thesis under the [Multimedia Understanding Group](https://mug.ee.auth.gr/) 🔗 lab of the Electrical and Computer Engineering Department of Aristotle University of Thessaloniki.

## Thesis: [Estimation of Food Intake Quantity Using Inertial Signals from Smartwatches](https://ikee.lib.auth.gr/record/360195/?ln=en) 🔗

#### Short Abstract
> Accurate monitoring of eating behavior is crucial for managing obesity and eating disorders such as bulimia nervosa. At the same time, existing methods rely on multiple and/or specialized sensors, greatly harming adherence and ultimately, the quality and continuity of data. This paper introduces a novel approach for estimating the weight of a ready-to-be-consumed quantity of food, already collected by a utensil (to be called as a *bite*), from a commercial smartwatch. We collected smartwatch inertial data from 10 participants, along with their associated bites through manual annotation and weights using a smart scale, under semi-controlled conditions. The proposed method combines behavioral features such as the time required to load the utensil with food, with statistical features of inertial signals, that serve as input to a Support Vector Regression model to predict bite weights. Under a leave-one-subject-out cross-validation scheme, our approach achieves a **mean absolute error (MAE) of 3.81 grams per bite**, representing a 20.13% improvement in relative MAE compared to a baseline model that predicts using the mean bite weight of the training subjects. In contrast, an adapted state-of-the-art method shows a -30.72% performance against the same baseline. The results presented in this work establish the feasibility of extracting meaningful bite weight estimates from commercial smartwatch inertial sensors alone, laying the groundwork for future accessible, non-invasive dietary monitoring systems.

<details>
  <summary>Extended Abstract (Click to expand)</summary>
  
> Monitoring food intake is fundamental for health and weight management, particularly for individuals with eating disorders, diabetes, and obesity. However, existing methods rely on manual logging, specialized equipment, or multiple intrusive devices, limiting their practical application. This thesis proposes a novel method for estimating bite weight using exclusively inertial data from a commercially available smartwatch, offering a non-intrusive and an accessible solution.
> 
> A comprehensive data collection system was developed, comprising custom applications for smartphone and smartwatch integration. Data was collected from 23 participants over 9.64 hours, encompassing both free-living and semi-supervised eating conditions. A subset was meticulously annotated for ground truth, synchronizing signals and labeling 342 bite events (defined as the temporal interval from the initiation of food collection to the completion of downward hand movement following food insertion in the mouth) with their corresponding weights. Following an extensive literature review, we implemented a micromovement detection algorithm achieving 0.823 average accuracy, based on Kyritsis *et al.*'s work, to extract meaningful eating behavior insights. Additionally, we derived statistical features from the inertial data during bite events using established techniques. Through systematic feature extraction and evaluation, combining behavioral and statistical elements, an optimal 6-feature vector was derived. For bite weight estimation, a Support Vector Regression (SVR) model with linear kernel was developed, optimized through established and systematic fine-tuning techniques.
> 
> The proposed method was evaluated through three complementary experiments under a common leave-one-subject-out cross-validation framework. The primary bite weight estimation experiment demonstrated the SVR model's effectiveness, achieving a mean absolute error of 4.11 grams and 14.44% improvement over the baseline model. Comparative analysis with four advanced deep learning architectures confirmed the systematic superiority of the proposed SVR model, which exhibited the highest improvement (12.87%) over the baseline model and the lowest mean absolute percentage error (44.32%). Finally, the proposed algorithm was evaluated against the state-of-the-art method from the literature, and was adapted to our dataset, utilizing data only from the dominant hand. Results demonstrated our method's superiority across all evaluation metrics, achieving 20.13% improvement over the baseline model, compared to the negative improvement (-30.72%) of the literature's algorithm, and attaining a mean absolute error of 3.81 grams per bite versus 6.23 grams, establishing the effectiveness of the proposed approach in the context of single-device bite weight prediction.
> 
> To the best of our knowledge, this is the first study to estimate bite weight using only inertial data from a smartwatch. As a feasibility study, it demonstrates that with certain assumptions and careful feature engineering, meaningful results are attainable, achieving comparable performance to existing multisensor, multimodal methods while reducing hardware requirements. The successful extraction of bite weight estimation from a smartwatch's inertial data, opens new avenues in the field of food intake tracking and lays the groundwork for future research and development.
</details>

## Project Overview

- **Smartphone App (`app` folder):**  
  Coordinates data collection, manages communication with wearables, and handles server uploads and is the main and only interacting point for the collection process.

- **Smartwatch App (`wear` folder):**  
  Gathers high-frequency accelerometer and gyroscope data (~52 Hz), splits files, monitors battery, and ensures reliable syncing with the phone.

- **Sensor Capture Module (`sensorcapture` folder):**  
  Centralizes sensor-handling logic, with robust data collection, storage, and retrieval routines.

- **Mandometer (Legacy, `mandometer` folder):**  
  Integrates a Bluetooth scale for optional weight measurements. Code adapted and integrated from an older project.

- **Audio (`audio` folder):**
  Handles audio capture logic.

## Key Features

1. **Multi-Device Integration**  
   - Seamless data exchange over Bluetooth Low Energy.  
   - Automatic reconnection and ACK-based retry mechanisms.

2. **High-Frequency Sensing**  
   - Raw IMU data (~51.56 Hz) on Wear OS.  
   - File splitting for manageable chunks and easier handling.

3. **Data Reliability & Storage**  
   - Local `Room` database on each device for safe caching and status tracking.  
   - Timestamps and unique naming to prevent duplication.
   - Retrofit-based server uploads with status tracking.

4. **Power Efficiency**  
   - Background services, partial wake locks, custom adaptive battery logic.  
   - Minimal watch UI to reduce CPU overhead.

5. **Remote Control**  
   - Start/stop wearable recordings from the phone.  
   - Optional Mandometer pairing for weight data capture.  
 

## Technologies & Methods

- **Multithreading & Concurrency**: Executors, semaphores, locks for parallel tasks.  
- **Networking**: Bluetooth Low Energy (BLE), `Retrofit` for server communication, `DataLayerAPI` for bilateral app communication.  
- **Databases**: `Room` for local caching and metadata tracking.  
- **Sensors & Hardware**: Accelerometer, Gyroscope, Bluetooth scale.  
- **Android Lifecycle & Components**: Activities, Services, Workers, BroadcastReceivers.  
- **Notifications & UI**: Minimal watch display, phone UI with `ConstraintLayout`.  
- **Testing & Debugging**: Logging, wearable battery monitoring, retry strategies.

## Acknowledgments
- This project was made under the [Multimedia Understanding Group](https://mug.ee.auth.gr/) 🔗 lab of Aristotle University of Thessaloniki.
- Audio capture logic and core smartphone app was created by my colleague Georgios Tsakiridis for his [thesis](https://ikee.lib.auth.gr/record/356498/files/Tsakiridis_Georgios.pdf) 🔗 under the same lab.

For further inquiries regarding the applications, my thesis, feel free to contact me at [ioanlevi@ece.auth.gr](mailto:ioanlevi@ece.auth.gr).

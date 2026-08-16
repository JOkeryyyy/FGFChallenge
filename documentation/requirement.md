# "The AI Semantic Log"

## Android Take Home Challenge

### The Overview

You are tasked with building a prototype for an **AI-Driven Log Viewer**. The app will fetch a large dataset of unstructured log entries (simulating AI-generated events), process them for efficient searching, and display them using a high-performance, polished UI.

### Core Requirements

#### 1. Architecture & Networking (The Foundation)

- **Clean Architecture:** Implement a multi-module (or clearly package-separated) approach using **MVVM** or **MVI**.
- **Networking:** Use **Retrofit/OkHttp** to fetch a JSON payload from [this endpoint](https://firebasestorage.googleapis.com/v0/b/fieldinspectiondev.firebasestorage.app/o/data%2Flogs_5k.json?alt=media&token=15c66bf6-9716-44da-b3d1-ba9bb241baf8).
- **DI:** Use **Hilt** or **Koin** for dependency injection.
- **Concurrency:** Use **Kotlin Coroutines and Flow** for all asynchronous operations.

#### 2. Data Filtering

- **Efficient Filtering:** The log dataset contains 5,000+ entries. Implement search feature to allow for near-instant "search-as-you-type" functionality across logs.
- **Data Transformation:** Convert the raw API response into a UI-ready model that groups logs by "Session ID" or "Timestamp"..

#### 3. UI/UX (Jetpack Compose)

- **Pixel-Perfect List:** Build a smooth-scrolling list.
- **Custom Component:** Create a custom "Severity Indicator" component using **Compose Canvas** (e.g., a multi-color ring or a shaded graph) that reflects the density of error logs.
- **Interactive States:** Implement a "Details" sheet for the log details. .

#### 4. AI & Modern Workflow

- **AI Implementation:** We encourage the use of AI tools (Cursor, ChatGPT, Claude) for this challenge.  
  **Requirement:** Must include a `PROMPTS.md` file in your repo documenting all instances where you used AI to accelerate development.
- **Unit Testing:** Write robust Unit tests for the core business logic and ViewModel state.

### Delivery Instructions

1. Please ensure the code is readable and provide enough documentation through comments.
2. Include the `readme.md` file with high instructions to set up this project as well as an App Screen Recording showcasing App features in the readme file.
3. Include the `PROMPTS.md` file with the info as mentioned above.
4. Push your code to Github and send us the link.

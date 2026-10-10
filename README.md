# 🐧 Credity - Smart Debt Prevention & Financial Health App

**Credity** is an intelligent, real-time Android financial management application designed to protect users from credit card debt, forecast interest penalty risks, and maintain a healthy credit score. Powered by **Penny the Penguin 🐧** and **Grafilab AI**, Credity provides live budget tracking, dynamic Debt Score calculations, interactive spending evaluations, and an Android App Widget.

---

## 🌟 Key Features

### 1. 🏠 Dynamic Home Screen & ATM Keypad
* **Live "Safe To Spend" Budget:** Evaluates remaining monthly safe funds ($\text{Income} - \text{Fixed Expenses} - \text{Spent}$) in real-time.
* **Color Transition Meter:** Interpolates screen gradient colors from **Forest Green 🟢** (Full Buffer) to **Amber 🟡** and **Dark Red 🔴** as the budget depletes.
* **ATM-Style Keypad:** Allows typing potential purchase amounts to test affordability before spending.
* **Set Daily Left Limit:** Calculates daily safe allowance as $\frac{\text{Income} - \text{Fixed Expenses}}{31}$, deducting confirmed purchases directly with a floor at `0`.

### 2. 🚨 Spend Result & 24% Interest Risk Evaluation
* **Fast AI Evaluation:** Generates on-the-spot spending forecasts when checking purchase amounts.
* **Conditional 24% Interest Penalty Card:** If a requested purchase causes a negative safe budget, Credity calculates the exact 1st-month 24% APR interest penalty ($\text{Overspent Amount} \times 24\%$).
* **Interactive AI Chatbot:** Allows asking follow-up questions directly on the result page with typewriter text animations.

### 3. 🐧 Penny the Penguin AI Chatbot (Powered by Grafilab)
* **Grafilab Gemini Integration:** Connects to `https://llm.grafilab.ai/v1/chat/completions` using model `gemini/gemini-3.6-flash`.
* **Dynamic 3-Question Profiling:** Asks personalized financial questionnaire sets on launch without generic greetings.
* **Typewriter Text Animation:** Character-by-character streaming text reveal for high-tech AI interaction.
* **Live Spreadsheet Context:** Integrates live transaction history feeds from Google Spreadsheets.

### 4. 📄 Financial Statement Import & AI Noise Filtering
* **PDF / File Statement Picker:** Upload bank statements or transaction CSV files in Settings.
* **AI Capital Noise Removal:** Penny AI parses statement deposits and recurring bills while filtering out 1-time capital purchases (laptops, phones, appliances, vacations).
* **One-Tap Budget Sync:** Applies extracted income and recurring expenses directly into app state and Firebase.

### 5. 📱 Aesthetic Android App Widget
* **Custom Penguin Layout:** Displays current Debt Score number directly above Penny the Penguin 🐧, current status label (`"Healthy"`, `"Watch it"`, `"Slow down"`), and Daily Allowance Left.
* **Persistent SharedPreferences Storage:** Maintains accurate score and daily limit display even when the app is exited.
* **Direct Launcher:** Tapping the widget launches directly into Penny's AI Chatbot screen.

---

## 📐 Mathematical Formulas

1. **Base Safe Buffer ($S_{base}$):**
   $$S_{base} = \text{Monthly Income} - \text{Fixed Expenses}$$

2. **Remaining Safe Budget ($S_{remaining}$):**
   $$S_{remaining} = \text{Monthly Income} - \text{Fixed Expenses} - \text{Total Spent This Month}$$

3. **Sensitive Debt Score ($0–100$):**
   $$\text{Score} = \begin{cases} 
      100 & \text{if } S_{remaining} \ge S_{base} \\
      20 + 80 \times \left(\frac{S_{remaining}}{S_{base}}\right) & \text{if } 0 \le S_{remaining} < S_{base} \\
      \text{clamp}\left(20 + 20 \times \left(\frac{S_{remaining}}{S_{base}}\right), 0, 20\right) & \text{if } S_{remaining} < 0 
   \end{cases}$$

4. **Base Daily Allowance ($D_{base}$):**
   $$D_{base} = \frac{\text{Monthly Income} - \text{Fixed Expenses}}{31}$$

5. **Daily Left Value:**
   $$\text{Daily Left} = \text{max}\left(0.00, D_{base} - \text{Total Spent Today}\right)$$

6. **First-Month 24% Interest Penalty:**
   $$\text{Interest Penalty Cost} = \text{Overspent Amount} \times 0.24$$

---

## 🛠️ Architecture & Tech Stack

* **Language & Framework:** Kotlin, Jetpack Compose, Material 3
* **Concurrency:** Kotlin Coroutines & `StateFlow`
* **Cloud Persistence:** Firebase Firestore (`FirebaseRepository.kt`)
* **Background Worker:** Android `WorkManager` (`SpreadsheetSyncWorker.kt`)
* **Widget System:** `AppWidgetProvider` & `RemoteViews` (`CredityWidgetProvider.kt`)
* **AI Service:** Grafilab Gemini API (`GrafilabAiService.kt`)

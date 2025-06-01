FactShare :

FactShare is a web application with a Node.js backend, React frontend, and two Python Flask apps for news verification and a chatbot, using MongoDB as the database.
Prerequisites
Ensure the following are installed:

Node.js (v14+): Check with node -v and npm -v
Python 3 (v3.8+): Check with python3 --version and pip3 --version
MongoDB Community Edition (v4.4+): Check with mongod --version
Git: Check with git --version
Homebrew (macOS, optional): Install via brew.sh

For Windows/Linux, follow MongoDB’s installation guide.
Setup

# Install MongoDB (macOS)

brew tap mongodb/brew
brew install mongodb-community
brew services start mongodb-community

# Clone repository

git clone https://github.com/your-username/factShare.git
cd factShare

# Setup Backend

cd backend
npm install

# Setup Frontend

cd ../frontend
npm install

# Setup Python environment

cd src
python3 -m venv venv
source venv/bin/activate # Windows: venv\Scripts\activate
pip install -r requirements.txt

# Return to root

cd ../../

Note: Configure .env files in backend and frontend/src for ports and other settings. Refer to .env.example if provided.
Running the Project
Run each service in a separate terminal:

Backend (Node.js)  
cd backend
npm start

Runs at: http://localhost:5000 (or PORT in backend/.env)

Frontend (React)  
cd frontend
npm start

Runs at: http://localhost:3000

Flask News Verification (app.py)  
cd frontend/src
source venv/bin/activate # Windows: venv\Scripts\activate
flask --app app run --port 5001

Runs at: http://localhost:5001 (or FLASK_APP_PY_PORT)

Flask Chatbot (bot.py)  
cd frontend/src
source venv/bin/activate # Windows: venv\Scripts\activate
flask --app bot run --port 8000

Runs at: http://localhost:8000 (or FLASK_BOT_PY_PORT)

Stopping the Project

MongoDB (macOS): brew services stop mongodb-communityWindows/Linux: Use sudo systemctl stop mongod or equivalent.
Services: Press Ctrl + C in each terminal.
Virtual Environment: Run deactivate.

Troubleshooting

Port Conflicts: Check with lsof -i :PORT and update .env if needed.
MongoDB Issues: Ensure it’s running (mongod or brew services start mongodb-community).
Python Dependencies: Verify requirements.txt exists in frontend/src.

For more details, check the GitHub repository.

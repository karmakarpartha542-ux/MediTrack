# MediTrack — Hospital Management System

MediTrack is a Hospital Management System developed using Java and TypeScript. It is designed to manage patients, doctors, appointments, hospital rooms, and billing through a simple web-based interface.

## Features

- **Dashboard:** Overview of hospital activities.
- **Patient Management:** Manage patient information.
- **Doctor Management:** Maintain doctor records and specializations.
- **Appointment Management:** Manage patient appointments.
- **Room Management:** Manage general wards, cabins, and ICU rooms.
- **Billing Management:** Organize bills and billing information.
- **File-Based Storage:** Store application data using JSON files.
- **Web Interface:** Responsive frontend built with HTML, CSS, and TypeScript.

## Technologies Used

- Java
- Object-Oriented Programming (OOP)
- HTML
- CSS
- TypeScript
- JSON
- Git and GitHub

## Project Structure

```text
MediTrack/
├── src/                 # Java backend source code
├── frontend/            # Frontend application
│   ├── assets/
│   ├── css/
│   ├── dist/
│   └── src/
├── data/                # Demo JSON data
├── sources.txt
├── .gitignore
└── README.md
```

## Requirements

- Java JDK
- Node.js and npm
- Git

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/karmakarpartha542-ux/MediTrack.git
cd MediTrack
```

### 2. Compile the Java Backend

```bash
javac -d bin @sources.txt
```

### 3. Run the Backend

```bash
java -cp bin com.meditrack.Main
```

The backend uses port `8080` by default.

### 4. Run the Frontend

Open another terminal:

```bash
cd frontend
npm install
npm run build
npm run serve
```

Open the local URL shown in the terminal, usually `http://localhost:3000`.

## Data Storage

The application uses JSON files in the `data/` directory for file-based data storage. The repository contains demonstration data.

## Project Goal

The goal of MediTrack is to demonstrate Java OOP concepts and build a practical hospital management application with a web-based frontend.

## Contributors

Developed as an academic software project.

## License

This project is currently intended for educational and demonstration purposes.

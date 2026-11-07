@echo off
title Manchorati Admin Dashboard
color 0B
echo ===================================================
echo   Manchorati Admin Dashboard - Lawhat Tahakom
echo ===================================================
echo.
echo Starting dashboard server...
start http://localhost:5000
python app.py
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to start Python server. Make sure Python and Flask are installed.
    pause
)

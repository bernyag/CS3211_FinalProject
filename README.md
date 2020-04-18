# CS3211_Project
Final project for the course CS3211 - Parallel and Concurrent Programming

This project is concerned with implementing a java based web-scraper. Assertions, such as the non-existence of deadlocks/data races are verified with PAT (Process Analysis Toolkit). 

## Java web scraper

## PAT mdoel
PAT (Process Analysis Toolkit) is a toolkit developed at the National University of Singapore (NUS) with the intent of virefying properties, such as deadlock freeness, of a software system. A model which captures the behavoiur of the system is developed in PAT, whereafter assertions are evaluated. The PAT model illustrates the problemacies related to not establishing a critical session whilst writing to the indexed URL tree and gives the traces of execution assocciated with the violation of uniquness of records in the tree.

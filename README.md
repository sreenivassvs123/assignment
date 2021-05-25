##Step-by-step instructions to run

This application is written in java and uses spring boot libraries. This requires minimum java 8 to run.

After downloading the repo, application can be run in either of below 2 ways
1. Run the assignment jar using command  `java -jar assignment.jar`
2. `./gradlew clean build bootrun`

In either of the above 2 approaches, the scheduled method, getDebts, in DebtService class prints the required debts
 information every 5 minutes by calling the true accord api endpoints.
 

##How this code was written
1. Used spring boot framework to create the service.
2. Used feign client to make http requests to true accord endpoints.
3. Business logic is written in class `DebtService` under package `com.example.assignment.debt`.
4. Unit tests were written in class `DebtServiceTest` for all the use-cases that I envisioned.

###Assumptions made
1. Debts will be associated with maximum of one payment plan.

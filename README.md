# CLI-tool-for-SQL
##This application uses JDBC to interact with a SQL script and update a particular url.csv that it is referencing. Wrote it for fun to practice JAVA and get some experience with JDBC.

### Repo has both CLI and GUI interface for the applicaiton.

## Key Functionalities include :
1. The user can view the contents of each table.
2. The user can search by PUBLICATIONID and return all attributes from PUBLICATIONS table, along with an additional field indicating the total number of authors for the paper in the search results.
3. The user can update a URL in the PUBLICATIONS table with the corresponding URL obtained from url.csv by providing the PUBLICATIONID. After the update, the resultingb updated tuple is displayed.
4. No type of invalid input can cause the program to crash.
5. The required paper.sql file and url.csv file present in the *req* folder.

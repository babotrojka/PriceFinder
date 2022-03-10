# PriceFinder

Price Finder is a Java application used to find best discounts from catalogues of nearby supermarkets. 

It uses **AWS Textract** to extract text from each page of catalogue. Textract returns text and position of each text on an image.

Using position, I used **kmeans** algorithm to group discounts, prices and names of articles on each page. After the grouping, I extract information about each article and print it to file *out.txt*

Program can be run as a classic java application
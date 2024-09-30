package com.insertbooks.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jdk.jpackage.internal.Log;

import java.util.Map;

public class BookHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        String httpMethod = request.getHttpMethod();

        switch (httpMethod) {
            case "GET":
                return getBookById(request, context);
            case "POST":
                return saveBook(request, context);
            case "PUT":
                return updateBook(request, context);
            case "DELETE":
                return deleteBook(request, context);
            default:
                return new APIGatewayProxyResponseEvent().withStatusCode(405).withBody("Method Not Allowed");
        }


    }

    public APIGatewayProxyResponseEvent deleteBook(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> pathParameters = request.getPathParameters();
        String bookId = pathParameters.get("bookId");

        Book bookToDelete = dynamoDBMapper.load(Book.class, bookId);

        if (bookToDelete != null) {
            dynamoDBMapper.delete(bookToDelete);
            return new APIGatewayProxyResponseEvent().withStatusCode(200).withBody("Book deleted successfully!");
        } else {
            return new APIGatewayProxyResponseEvent().withStatusCode(404).withBody("Book not found!");
        }
    }

    public APIGatewayProxyResponseEvent updateBook(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> pathParameters = request.getPathParameters();
        String bookId = pathParameters.get("bookId");

        try {
            Book existingBook = dynamoDBMapper.load(Book.class, bookId);
            if (existingBook == null) {
                return new APIGatewayProxyResponseEvent().withStatusCode(404).withBody("Book not found!");
            }

            Book updatedBook = objectMapper.readValue(request.getBody(), Book.class);
            updatedBook.setId(bookId);

            dynamoDBMapper.save(updatedBook);

            JsonObject returnValue = new JsonObject();
            returnValue.addProperty("Message", "Book updated successfully!");

            APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
            response.withStatusCode(200).withBody(returnValue.toString());


            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error updating book!");
        }
    }

    public APIGatewayProxyResponseEvent saveBook(APIGatewayProxyRequestEvent request, Context context) {
        Book book = new Book();
        try {
            book = objectMapper.readValue(request.getBody(), Book.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        dynamoDBMapper.save(book);

        JsonObject returnValue = new JsonObject();
        returnValue.addProperty("Message", "Book saved successfully!");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        response.withStatusCode(200).withBody(returnValue.toString());

        return response;
    }

    public APIGatewayProxyResponseEvent getBookById(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> pathParameters = request.getPathParameters();
        String bookId = pathParameters.get("bookId");

        Book book = dynamoDBMapper.load(Book.class, bookId);

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        if (book != null) {
            Gson gson = new Gson();
            JsonObject returnValue = gson.toJsonTree(book).getAsJsonObject();

            response.withStatusCode(200).withBody(returnValue.toString());
        } else
            response.withStatusCode(500).withBody("Book not found!");

        return response;
    }
}
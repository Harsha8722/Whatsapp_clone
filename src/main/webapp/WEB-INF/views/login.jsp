<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html>

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Login - WhatsApp Clone</title>
        <link rel="stylesheet" href="/css/style.css">
    </head>

    <body>
        <div class="auth-container">
            <div class="auth-box">
                <div class="auth-header">
                    <h1>WhatsApp Clone</h1>
                    <p>Login to continue</p>
                </div>

                <% if (request.getAttribute("error") !=null) { %>
                    <div class="error-message">
                        <%= request.getAttribute("error") %>
                    </div>
                    <% } %>

                        <% if (request.getAttribute("success") !=null) { %>
                            <div class="success-message">
                                <%= request.getAttribute("success") %>
                            </div>
                            <% } %>

                                <form action="/login" method="post" class="auth-form">
                                    <div class="form-group">
                                        <label for="phone">Phone Number</label>
                                        <div class="input-group">
                                            <i class="fas fa-phone-alt"></i>
                                            <input type="text" id="phone" name="phone" placeholder="Enter phone number"
                                                required>
                                        </div>
                                    </div>

                                    <div class="form-group">
                                        <label for="password">Password</label>
                                        <div class="input-group">
                                            <i class="fas fa-lock"></i>
                                            <input type="password" id="password" name="password"
                                                placeholder="Enter password" required>
                                        </div>
                                    </div>

                                    <button type="submit" class="btn-primary">Login</button>
                                </form>

                                <div class="auth-footer">
                                    <p>Don't have an account? <a href="/register">Register here</a></p>
                                </div>
            </div>
        </div>
    </body>

    </html>
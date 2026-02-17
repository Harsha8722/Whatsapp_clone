<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
    <!DOCTYPE html>
    <html>

    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Register - WhatsApp</title>
        <link rel="stylesheet" href="/css/style.css">
    </head>

    <body>
        <div class="auth-container">
            <div class="auth-box">
                <div class="auth-header">
                    <h1>WhatsApp Clone</h1>
                    <p>Create your account</p>
                </div>

                <% if (request.getAttribute("error") !=null) { %>
                    <div class="error-message">
                        <%= request.getAttribute("error") %>
                    </div>
                    <% } %>

                        <form action="/register" method="post" class="auth-form">
                            <div class="form-group">
                                <label for="name">Full Name</label>
                                <div class="input-group">
                                    <i class="fas fa-user"></i>
                                    <input type="text" id="name" name="name" placeholder="Enter your name" required>
                                </div>
                            </div>

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
                                    <input type="password" id="password" name="password" placeholder="Create password"
                                        required>
                                </div>
                            </div>

                            <button type="submit" class="btn-primary">Register</button>
                        </form>

                        <div class="auth-footer">
                            <p>Already have an account? <a href="/login">Login here</a></p>
                        </div>
            </div>
        </div>
    </body>

    </html>
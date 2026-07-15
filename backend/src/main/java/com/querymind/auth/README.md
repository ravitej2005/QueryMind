# auth module

Responsibility: user registration/login, JWT issuance (access + refresh),
refresh rotation with reuse detection, logout / logout-everywhere.

Public service interface: none exposed cross-module yet (AuthService is
consumed only by AuthController in this module). JwtService/JwtAuthFilter
are shared security infra used by the global SecurityConfig.

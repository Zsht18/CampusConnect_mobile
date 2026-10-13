<?php
// db_connect.php
// Update these variables with your Hostinger database details!

$host = "localhost";
$db_name = "u793073111_campusconnect"; // Usually starts with a u + numbers
$username = "u793073111_campusconnect";
$password = "CampusConnect1210713";

try {
    $conn = new PDO("mysql:host={$host};dbname={$db_name}", $username, $password);
    $conn->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
} catch(PDOException $exception) {
    echo "Connection error: " . $exception->getMessage();
}
?>
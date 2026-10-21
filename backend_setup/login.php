<?php
// login.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$id_number = isset($_POST['id_number']) ? $_POST['id_number'] : '';
$password = isset($_POST['password']) ? $_POST['password'] : '';

if(!empty($id_number) && !empty($password)){
    // Fetch user from database and count their posts
    $query = "SELECT u.id, u.id_number, u.full_name, u.username, u.profile_pic_url, u.password_hash, u.created_at,
              (SELECT COUNT(*) FROM mobile_posts WHERE user_id = u.id) as posts_count
              FROM mobile_users u WHERE u.id_number = :id_number LIMIT 0,1";
    $stmt = $conn->prepare($query);
    $stmt->bindParam(':id_number', $id_number);
    $stmt->execute();

    $num = $stmt->rowCount();

    if($num > 0){
        $row = $stmt->fetch(PDO::FETCH_ASSOC);

        // Verify Password
        if(password_verify($password, $row['password_hash'])){
            // Login Success
            $user_arr = array(
                "id" => $row['id'],
                "id_number" => $row['id_number'],
                "full_name" => $row['full_name'],
                "username" => $row['username'],
                "profile_pic_url" => $row['profile_pic_url'],
                "created_at" => $row['created_at'],
                "posts_count" => (int)$row['posts_count']
            );

            http_response_code(200);
            echo json_encode(array(
                "success" => true,
                "message" => "Login successful.",
                "user" => $user_arr
            ));
        } else {
            // Wrong Password
            http_response_code(200);
            echo json_encode(array("success" => false, "message" => "Wrong password"));
        }
    } else {
        // User not found
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Wrong ID Number"));
    }
} else {
    // Bad request
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Incomplete data. Please fill in ID Number and Password."));
}
?>
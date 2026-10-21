<?php
// create_post.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$title = isset($_POST['title']) ? trim($_POST['title']) : '';
$category = isset($_POST['category']) ? trim($_POST['category']) : '';
$content = isset($_POST['content']) ? trim($_POST['content']) : '';
$is_anonymous = isset($_POST['is_anonymous']) ? intval($_POST['is_anonymous']) : 0;

if($user_id > 0 && !empty($title) && !empty($content)){
    $file_url = null;
    if(isset($_FILES['file']) && $_FILES['file']['error'] !== UPLOAD_ERR_OK){
        echo json_encode(array("success" => false, "message" => "Attachment upload failed (error " . intval($_FILES['file']['error']) . ")."));
        exit;
    }

    if(isset($_FILES['file'])){
        $upload_dir = __DIR__ . '/uploads/';
        if(!is_dir($upload_dir)){
            if(!mkdir($upload_dir, 0755, true)){
                echo json_encode(array("success" => false, "message" => "Unable to create attachment folder."));
                exit;
            }
        }

        $original_name = basename($_FILES['file']['name']);
        $extension = strtolower(pathinfo($original_name, PATHINFO_EXTENSION));
        $allowed_extensions = array('pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png');
        if(!in_array($extension, $allowed_extensions, true)){
            echo json_encode(array("success" => false, "message" => "Unsupported attachment type."));
            exit;
        }

        $filename = 'post_' . $user_id . '_' . uniqid('', true) . '.' . $extension;
        $file_path = $upload_dir . $filename;
        if(!move_uploaded_file($_FILES['file']['tmp_name'], $file_path)){
            echo json_encode(array("success" => false, "message" => "Unable to save attachment."));
            exit;
        }

        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
        $script_directory = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])), '/');
        $file_url = $protocol . "://" . $_SERVER['HTTP_HOST'] . $script_directory . "/uploads/" . $filename;
    }

    $query = "INSERT INTO mobile_posts (user_id, title, category, content, file_url, is_anonymous) VALUES (:user_id, :title, :category, :content, :file_url, :is_anonymous)";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(':user_id', $user_id);
    $stmt->bindParam(':title', $title);
    $stmt->bindParam(':category', $category);
    $stmt->bindParam(':content', $content);
    $stmt->bindParam(':file_url', $file_url);
    $stmt->bindParam(':is_anonymous', $is_anonymous);

    if($stmt->execute()){
        http_response_code(200);
        echo json_encode(array(
            "success" => true,
            "message" => $file_url !== null ? "Post created with attachment." : "Post created successfully.",
            "file_url" => $file_url
        ));
    } else {
        http_response_code(200);
        echo json_encode(array("success" => false, "message" => "Unable to create post."));
    }
} else {
    http_response_code(200);
    echo json_encode(array("success" => false, "message" => "Incomplete post data. Please enter Title and Content."));
}
?>
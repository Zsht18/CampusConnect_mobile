<?php
// create_post.php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");
header("Access-Control-Allow-Methods: POST");

include_once 'db_connect.php';
require_once __DIR__ . '/MobileAiModeration.php';

$user_id = isset($_POST['user_id']) ? intval($_POST['user_id']) : 0;
$title = isset($_POST['title']) ? trim($_POST['title']) : '';
$category = isset($_POST['category']) ? trim($_POST['category']) : '';
$content = isset($_POST['content']) ? trim($_POST['content']) : '';
$is_anonymous = isset($_POST['is_anonymous']) ? intval($_POST['is_anonymous']) : 0;

if($user_id > 0 && !empty($title) && !empty($content)){
    $uploaded_urls = array();

    // Check if multiple files were uploaded in $_FILES['files']
    if (isset($_FILES['files']) && is_array($_FILES['files']['name'])) {
        $upload_dir = __DIR__ . '/uploads/';
        if(!is_dir($upload_dir)){
            mkdir($upload_dir, 0755, true);
        }

        $allowed_extensions = array('pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png');
        $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
        $script_directory = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])), '/');

        for ($i = 0; $i < count($_FILES['files']['name']); $i++) {
            if ($_FILES['files']['error'][$i] === UPLOAD_ERR_OK) {
                $original_name = basename($_FILES['files']['name'][$i]);
                $original_name = preg_replace('/[^A-Za-z0-9._ -]/', '_', $original_name);
                if (empty($original_name)) $original_name = 'attachment';

                $extension = strtolower(pathinfo($original_name, PATHINFO_EXTENSION));
                if (in_array($extension, $allowed_extensions, true)) {
                    $upload_key = 'post_' . $user_id . '_' . uniqid('', true);
                    $post_upload_dir = $upload_dir . $upload_key . '/';
                    if (mkdir($post_upload_dir, 0755, true)) {
                        $file_path = $post_upload_dir . $original_name;
                        if (move_uploaded_file($_FILES['files']['tmp_name'][$i], $file_path)) {
                            $url = $protocol . "://" . $_SERVER['HTTP_HOST'] . $script_directory . "/uploads/" . $upload_key . "/" . rawurlencode($original_name);
                            $uploaded_urls[] = $url;
                        }
                    }
                }
            }
        }
    } else if (isset($_FILES['file'])) {
        // Fallback for single file upload
        $upload_dir = __DIR__ . '/uploads/';
        if(!is_dir($upload_dir)){
            mkdir($upload_dir, 0755, true);
        }

        $original_name = basename($_FILES['file']['name']);
        $original_name = preg_replace('/[^A-Za-z0-9._ -]/', '_', $original_name);
        if(empty($original_name)) $original_name = 'attachment';

        $extension = strtolower(pathinfo($original_name, PATHINFO_EXTENSION));
        $allowed_extensions = array('pdf', 'doc', 'docx', 'jpg', 'jpeg', 'png');
        if (in_array($extension, $allowed_extensions, true)) {
            $upload_key = 'post_' . $user_id . '_' . uniqid('', true);
            $post_upload_dir = $upload_dir . $upload_key . '/';
            if (mkdir($post_upload_dir, 0755, true)) {
                $file_path = $post_upload_dir . $original_name;
                if (move_uploaded_file($_FILES['file']['tmp_name'], $file_path)) {
                    $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
                    $script_directory = rtrim(str_replace('\\', '/', dirname($_SERVER['SCRIPT_NAME'])), '/');
                    $uploaded_urls[] = $protocol . "://" . $_SERVER['HTTP_HOST'] . $script_directory . "/uploads/" . $upload_key . "/" . rawurlencode($original_name);
                }
            }
        }
    }

    $file_url = count($uploaded_urls) > 0 ? implode(",", $uploaded_urls) : null;

    $query = "INSERT INTO mobile_posts (user_id, title, category, content, file_url, is_anonymous) VALUES (:user_id, :title, :category, :content, :file_url, :is_anonymous)";
    $stmt = $conn->prepare($query);

    $stmt->bindParam(':user_id', $user_id);
    $stmt->bindParam(':title', $title);
    $stmt->bindParam(':category', $category);
    $stmt->bindParam(':content', $content);
    $stmt->bindParam(':file_url', $file_url);
    $stmt->bindParam(':is_anonymous', $is_anonymous);

    if($stmt->execute()){
        $postId = (int)$conn->lastInsertId();
        $moderation = new MobileAiModeration($conn);
        $review = $moderation->inspect($title . "\n" . $content);
        $moderation->recordPost($postId, $review);
        http_response_code(200);
        echo json_encode(array(
            "success" => true,
            "message" => $file_url !== null ? "Post created with " . count($uploaded_urls) . " attachment(s)." : "Post created successfully.",
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
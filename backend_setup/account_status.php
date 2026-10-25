<?php
// account_status.php - shared helpers for suspended/banned accounts set from the admin dashboard.

const CC_RESTRICTION_MESSAGE = "Your account has been %s in violation of the CampusConnect community guidelines.";

// Returns 'suspended', 'banned', or '' when the matching account is in good standing.
function cc_account_restriction(PDO $conn, string $email = '', string $idNumber = '', string $username = ''): string
{
    try {
        $stmt = $conn->prepare(
            "SELECT m.status FROM mobile_users u
             JOIN admin_mobile_user_moderation m ON m.mobile_user_id = u.id
             WHERE m.status IN ('suspended','banned')
               AND (LOWER(u.institutional_email) = LOWER(:email) OR u.id_number = :id_number OR LOWER(u.username) = LOWER(:username))
             ORDER BY (m.status = 'banned') DESC LIMIT 1"
        );
        $stmt->execute([':email' => $email, ':id_number' => $idNumber, ':username' => $username]);
        $status = $stmt->fetchColumn();
        return $status ? (string)$status : '';
    } catch (Throwable $e) {
        // The moderation table does not exist until the admin dashboard has run once.
        return '';
    }
}

function cc_restriction_response(string $status): void
{
    http_response_code(200);
    echo json_encode(array(
        "success" => false,
        "account_status" => $status,
        "message" => sprintf(CC_RESTRICTION_MESSAGE, $status)
    ));
    exit();
}

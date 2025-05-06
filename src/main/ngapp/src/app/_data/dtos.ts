// Define an interface for the create discussion payload
export interface CreateDiscussionPayload {
  forumId: number;
  discussionId: number | null; // discussionId should be null initially
  title: string;
  comment: string;
  images: FileList | null;
  attachments: FileList | null;
}

// Define an interface for the registration payload (matches SignUpRequest without confirmPassword)
export interface RegistrationPayload {
  username: string;
  password?: string; // Password might be optional if handled differently, but usually required
  firstName: string;
  lastName: string;
  email: string;
}

// Define an interface for the backend's success/error message response
/* export interface MessageResponse {
  message: string;
} */

// Define an interface for the generic API response structure
export interface ApiResponse<T> {
  success: boolean;
  message?: string | null; // Optional fields match @JsonInclude(NON_NULL)
  data?: T | null;
  //errors?: { [key: string]: string } | null; // TypeScript equivalent of Map<String, String>
  errors?: string[] | null;
  timestamp: string;
}

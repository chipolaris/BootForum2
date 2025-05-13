// Model for the create discussion payload
export interface CreateDiscussionPayload {
  forumId: number;
  discussionId: number | null; // discussionId should be null initially
  title: string;
  comment: string;
  images: FileList | null;
  attachments: FileList | null;
}

// Model for the forum creation payload
export interface ForumDTO {
  id?: number;
  title: string;
  description: string;
  icon: string;
  iconColor: string;
  active: boolean;
}

export interface ForumGroupDTO {
  id?: number;
  title: string;
  icon: string;
  iconColor: string;
  forums?: ForumDTO[] | null;
  subGroups?: ForumGroupDTO[] | null;
}

// Model for the registration payload (matches SignUpRequest without confirmPassword)
export interface RegistrationPayload {
  username: string;
  password?: string; // Password might be optional if handled differently, but usually required
  firstName: string;
  lastName: string;
  email: string;
}

// Define an interface for the generic API response structure
export interface ApiResponse<T> {
  success: boolean;
  message?: string | null; // Optional fields match @JsonInclude(NON_NULL)
  data?: T | null;
  //errors?: { [key: string]: string } | null; // TypeScript equivalent of Map<String, String>
  errors?: string[] | null;
  timestamp: string;
}

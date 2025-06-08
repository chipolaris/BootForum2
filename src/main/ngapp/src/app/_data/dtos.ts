// Model for the create discussion payload
export interface DiscussionCreateDTO {
  forumId: number;
  title: string;
  content: string;
}

// Model for the forum creation payload
export interface ForumDTO {
  id?: number;
  title: string;
  description: string;
  icon: string;
  iconColor: string;
  active: boolean;
  stat: ForumStatDTO;
}

export interface ForumStatDTO {
  id?: number;
  lastComment: CommentInfoDTO;
  commentCount: number;
  discussionCount: number;
}

export interface CommentInfoDTO {
  id?: number;
  title: string;
  contentAbbr: string;
  commentId: number;
  commentor: string;
  commentDate: Date;
}

export interface ForumCreateDTO {
  title: string;
  description: string;
  icon: string;
  iconColor: string;
  active: boolean;
  parentGroupId: number | null;
}

export interface ForumUpdateDTO {
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

export interface ForumGroupCreateDTO {
  title: string;
  icon: string;
  iconColor: string;
  parentGroupId: number | null;
}

export interface ForumGroupUpdateDTO {
  id?: number;
  title: string;
  icon: string;
  iconColor: string;
}

/*
 * Used for display in the Forum Tree Table Component
 * The data is first level children of the root forum group: forums and forumGroups
 */
export interface ForumTreeTableDTO {
    forums?: ForumDTO[] | null;
    forumGroups?: ForumGroupDTO[] | null;
}

export interface Page<T> {
  content: T[];          // The actual data for the current page
  totalElements: number; // Total number of items across all pages
  totalPages: number;    // Total number of pages
  number: number;        // Current page number (usually 0-indexed from backend)
  size: number;          // Number of items per page
  first: boolean;        // True if this is the first page
  last: boolean;         // True if this is the last page
  numberOfElements: number; // Number of elements in the current page
  empty: boolean;        // True if the content array is empty
}

export interface ForumViewDTO {
  forumDTO: ForumDTO;
  // discussionDTOs: DiscussionDTO[]; // Old way
  discussionsPage: Page<DiscussionDTO>; // New: Paginated discussions
}

// Model for TagDTO, corresponding to Java's TagDTO
export interface TagDTO {
  id?: number;
  label: string;
  icon: string;
  iconColor: string;
  disabled: boolean;
}

// Model for FileInfoDTO, corresponding to Java's FileInfoDTO
export interface FileInfoDTO {
  id?: number; // Corresponds to Long id, optional as it might not be present before creation
  originalFilename: string;
  mimeType: string;
  path: string;
}

// You would also need CommentVoteDTO, for example:
export interface CommentVoteDTO {
  // Define fields for CommentVoteDTO based on Java DTO
  voteUpCount?: number;
  voteDownCount?: number;
}

export interface CommentDTO {
  id?: number; // Corresponds to Long id, optional as it might not be present before creation
  createDate: Date | string; // Date for client-side, string if received as ISO string
  createBy: string;
  updateDate?: Date | string | null; // Optional and nullable
  updateBy?: string | null;        // Optional and nullable
  title: string;
  content: string;
  replyToId?: number | null;     // Corresponds to Long replyToId, can be null
  ipAddress?: string | null;       // Optional and nullable
  attachments?: FileInfoDTO[] | null; // List of FileInfoDTO
  thumbnails?: FileInfoDTO[] | null;  // List of FileInfoDTO
  hidden: boolean;
  commentVote?: CommentVoteDTO | null; // Reference to CommentVoteDTO
}

// Model for DiscussionStatDTO, corresponding to Java's DiscussionStatDTO
export interface DiscussionStatDTO {
  commentCount?: number;
  viewCount?: number;
  lastViewed?: Date | string | null; // Date for client-side, string if received as ISO string
  thumbnailCount?: number;
  attachmentCount?: number;
  lastComment?: CommentInfoDTO | null;
  participants?: { [key: string]: number } | null; // Equivalent to Map<String, Integer>
}

// Model for DiscussionDTO, corresponding to Java's DiscussionDTO
export interface DiscussionDTO {
  id?: number; // Assuming id can be optional
  createDate: Date;
  createBy: string;
  title: string;
  content: string;
  attachments?: FileInfoDTO[] | null; // Array of FileInfoDTO
  thumbnails?: FileInfoDTO[] | null; // Array of FileInfoDTO
  tags?: TagDTO[] | null; // Array of TagDTO
  stat?: DiscussionStatDTO | null; // Reference to DiscussionStatDTO
}

export interface DiscussionSummaryDTO {
  id?: number;
  title: string;
  commentCount: number;
  viewCount: number;
  createDate: Date;
  createBy: string;
  lastCommentDate: Date;
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

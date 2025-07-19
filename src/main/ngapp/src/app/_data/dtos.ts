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
  lastDiscussion: DiscussionInfoDTO;
  commentCount: number;
  discussionCount: number;
}

export interface CommentInfoDTO {
  title: string;
  contentAbbr: string;
  commentId: number;
  commentor: string;
  commentDate: Date;
  discussionId: number;
  discussionTitle: string;
}

export interface CommentCreateDTO {
  discussionId: number;
  replyToId: number | null;
  title: string;
  content: string;
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
  fileSize: number;
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
  images?: FileInfoDTO[] | null;  // List of FileInfoDTO
  hidden: boolean;
  commentVote?: CommentVoteDTO | null; // Reference to CommentVoteDTO
}

// Model for the create discussion payload
export interface DiscussionCreateDTO {
  forumId: number;
  title: string;
  content: string;
}

// Model for DiscussionStatDTO, corresponding to Java's DiscussionStatDTO
export interface DiscussionStatDTO {
  commentCount?: number;
  viewCount?: number;
  lastViewed?: Date | string | null; // Date for client-side, string if received as ISO string
  imageCount?: number;
  attachmentCount?: number;
  lastComment?: CommentInfoDTO | null;
  participants?: { [key: string]: number } | null; // Equivalent to Map<String, Integer>
  voteUpCount?: number;
  voteDownCount?: number;
}

// Model for DiscussionDTO, corresponding to Java's DiscussionDTO
export interface DiscussionDTO {
  id?: number; // Assuming id can be optional
  createDate: Date;
  createBy: string;
  title: string;
  content: string;
  attachments?: FileInfoDTO[] | null; // Array of FileInfoDTO
  images?: FileInfoDTO[] | null; // Array of FileInfoDTO
  tags?: TagDTO[] | null; // Array of TagDTO
  stat?: DiscussionStatDTO | null; // Reference to DiscussionStatDTO
}

export interface DiscussionInfoDTO {
  discussionId: number;
  title: string;
  contentAbbr: string;
  discussionCreator: string;
  discussionCreateDate: Date;
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

export interface PersonDTO {
	id?: number;
	firstName?: string;
	lastName?: string;
	email?: string;
	createDate?: Date;
	updateDate?: Date;
	createdBy?: string;
	updatedBy?: string;
}

export interface UserDTO {
  id?: number;
  username: string;
  userRoles?: string[];
  accountStatus?: string;
  person: PersonDTO;
  createDate?: Date;
  updateDate?: Date;
  createdBy?: string;
  updatedBy?: string;
}

export interface UserProfileDiscussionDTO {
  discussionId: number;
  discussionTitle: string; // Note: backend DTO has Long, but string is more likely correct
  createdDate: string; // ISO date string
  title: string;
  content: string; // Truncated content
}

export interface UserProfileCommentDTO {
  commentId: number;
  createdDate: string; // ISO date string
  commentTitle: string;
  content: string; // Truncated content
  discussionId: number;
  discussionTitle: string;
}

export interface UserProfileDTO {
  username: string;
  firstName: string;
  lastName: string;
  joinDate: string; // ISO date string
  discussionCreatedCount: number;
  commentCount: number;
  imageUploaded: number;
  attachmentUploaded: number;
  reputation: number;
  profileViewed: number;
  lastLogin: string; // ISO date string
  comments: UserProfileCommentDTO[];
  discussions: UserProfileDiscussionDTO[];
}

// Model for the registration payload (matches SignUpRequest without confirmPassword)
export interface RegistrationPayload {
  username: string;
  password?: string; // Password might be optional if handled differently, but usually required
  firstName: string;
  lastName: string;
  email: string;
}

export interface SystemStatisticDTO {
  userCount: number;
  forumCount: number;
  discussionCount: number;
  commentCount: number;
  lastRegisteredUser: string;
  lastUserRegisteredDate: Date;
  lastComment: CommentInfoDTO;
  lastDiscussion: DiscussionInfoDTO;
}

/**
 * DTO for updating a user's personal information.
 * Corresponds to Java's PersonUpdateDTO.
 */
export interface PersonUpdateDTO {
  firstName: string;
  lastName: string;
  email: string;
}

/**
 * DTO for the password change payload.
 * Corresponds to Java's PasswordChangeDTO.
 */
export interface PasswordChangeDTO {
  oldPassword: string;
  newPassword: string;
  confirmNewPassword: string;
}

/**
 * DTO for the avatar upload response.
 * Corresponds to Java's AvatarDTO.
 */
export interface AvatarDTO {
  username: string;
  fileInfo: FileInfoDTO;
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

export function errorMessageFromApiResponse(apiResponse: ApiResponse<any>): string {

  let message = '';
  if (apiResponse.message) {
    message = apiResponse.message;
  }
  if (apiResponse.errors && apiResponse.errors.length > 0) {
    if(!message) { // Falsiness check: if message is empty, null, undefined, 0, false, or NaN
      // remove empty strings from errors and trim then assign to message
      message = apiResponse.errors.filter(e => e && e.trim()).join('\n');
    }
    else {
      // append message with errors
      message += '\n' + apiResponse.errors.filter(e => e && e.trim()).join('\n');
    }
  }

  return message;
}


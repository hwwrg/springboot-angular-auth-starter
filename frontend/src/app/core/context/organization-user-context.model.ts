export interface OrganizationContext {
  organizationId: string;
  organizationDisplayName: string;
  organizationStatus: string;
  workspaceId: string;
  workspaceCode: string;
  workspaceStatus: string;
  role: string;
}

export interface OrganizationMembership {
  organizationId: string;
  organizationDisplayName: string;
  organizationStatus: string;
  workspaceId: string;
  workspaceCode: string;
  role: string;
  status: string;
  primaryMembership: boolean;
}

export interface CurrentUserProfile {
  id: string;
  email: string;
  displayName: string;
  status: string;
  currentOrganization: OrganizationContext | null;
  memberships: OrganizationMembership[];
}

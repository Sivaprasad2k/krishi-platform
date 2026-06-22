import { tokenStorage } from '../../../core/storage/tokenStorage';

/**
 * Handles local logout session invalidation.
 * Clears cached storage tokens.
 */
export const logoutUser = () => {
  tokenStorage.removeToken();
};

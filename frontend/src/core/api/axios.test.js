import { describe, it, expect } from 'vitest';
import apiClient from './axios';

describe('apiClient Error Interceptor Tests', () => {
  it('exports valid axios client instance', () => {
    expect(apiClient).toBeDefined();
    expect(apiClient.defaults.baseURL).toBeDefined();
  });
});

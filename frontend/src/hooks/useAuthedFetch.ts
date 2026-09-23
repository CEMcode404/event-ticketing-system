import { useCallback } from "react";
import { useAuth0 } from "@auth0/auth0-react";
import { apiFetch } from "../lib/api";

export function useAuthedFetch() {
  const { getAccessTokenSilently } = useAuth0();

  return useCallback(
    async <T>(
      path: string,
      options: Omit<Parameters<typeof apiFetch<T>>[1], "token"> = {},
    ): Promise<T> => {
      const token = await getAccessTokenSilently();
      return apiFetch<T>(path, { ...options, token });
    },
    [getAccessTokenSilently],
  );
}
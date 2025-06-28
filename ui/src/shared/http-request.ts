import axios, { type AxiosResponse } from "axios";
import { useCallback, useMemo, useRef, useState } from "react";

export interface ServerObject<T> {
    isLoading: boolean;
    data: T | undefined;
    error: any;
    doGet(id?: string, options?: GetOptions): void;
    doCall(
        urlPart?: string,
        options?: Options
    ): Promise<void | AxiosResponse<T, any>>;
}
export interface GetOptions extends Options {
    cache?: boolean;
}
export interface Options {
    method?: HttpMethod;
    dataToSend?: unknown;
    controller?: AbortController;
}

export type HttpMethod = "GET" | "DELETE" | "POST" | "PUT" | "HEADER";

export const useServerObject = <T>(
    url: string,
    startValue?: T
): ServerObject<T> => {
    const [isLoading, setIsLoading] = useState<boolean>(false);
    const [data, setData] = useState<T | undefined>(startValue);
    const [error, setError] = useState<any>(undefined);

    const cache = useRef<Record<string, T>>({}); // In-memory cache

    const doGet = useCallback(
        (id?: string, options?: GetOptions) => {
            if (!options) options = {};
            options.controller = new AbortController();
            const requestUrl = url + (id ?? "");

            if (options?.cache && cache.current[requestUrl]) {
                setData(cache.current[requestUrl]);
            } else {
                doCall(id, options).then((r) => {
                    if (options?.cache && isAxiosResponse(r)) {
                        cache.current[requestUrl] = r.data;
                    }
                });
            }
            return () => options.controller?.abort();
        },
        [url]
    );

    const doCall = useCallback(
        (urlPart?: string, options?: Options) => {
            setError(undefined);
            setIsLoading(true);
            const requestUrl = url + (urlPart ?? "");

            return axios
                .request<T>({
                    baseURL: requestUrl,
                    data: options?.dataToSend,
                    method: options?.method || "GET",
                    signal: options?.controller?.signal,
                })
                .then((response) => {
                    setData(response.data);
                    return response;
                })
                .catch((e) => {
                    if (options?.controller?.signal.aborted) {
                        console.debug(requestUrl, "canceled", e);
                    } else {
                        console.error(requestUrl, e);
                        setError(e);
                    }
                })
                .finally(() => setIsLoading(false));
        },
        [url]
    );

    return useMemo(
        () => ({ isLoading, data, error, doGet, doCall }),
        [isLoading, data, error, doCall, url]
    );
};

export function isAxiosResponse<T = any>(
    value: unknown
): value is AxiosResponse<T> {
    return (
        typeof value === "object" &&
        value !== null &&
        "status" in value &&
        "data" in value
    );
}

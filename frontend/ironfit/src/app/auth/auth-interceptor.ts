import { HttpInterceptorFn } from '@angular/common/http';

export const authInterceptor: HttpInterceptorFn = (req, next) => {

    const token = localStorage.getItem('token');

    // NO enviar token al login
    if (token && !req.url.includes('/auth/login')) {

        const reqConToken = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });

        return next(reqConToken);
    }

    return next(req);
};
package com.ironfit.backendmongo.controlador.test;
import org.springframework.web.bind.annotation.*;

import com.ironfit.backendmongo.modelo.test.TestDoc;
import com.ironfit.backendmongo.repositorio.test.TestRepository;

import java.util.List;

@RestController
@RequestMapping("/api/test")

public class TestController {
    private final TestRepository repo;
    public TestController(TestRepository testRepo) {
        this.repo = testRepo;
    }

    @PostMapping
    public TestDoc crear(@RequestBody TestDoc body) {
        return repo.save(new TestDoc(body.getNombre()));
    }

    @GetMapping
    public List<TestDoc> listar() {
        return repo.findAll();
    }

    @GetMapping("/test")
    public String test() {
        return "API funcionando correctamente 🚀";
    }

}

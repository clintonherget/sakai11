package edu.nyu.classes.seats.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import edu.nyu.classes.seats.storage.migrations.BaseMigration;

public class SeatsStorage {

    public void runDBMigrations() {
        BaseMigration.runMigrations();
    }

}
